package com.fongmi.android.tv.player.mpv;

import android.net.Uri;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.FileUtil;
import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Prefers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.UnaryOperator;

/** Managed Lua scripts and mpv command buttons. */
public final class MpvScripts {

    public static final String PREFERENCE_KEY = "mpv_scripts";
    public static final int MAX_TITLE_LENGTH = 64;
    public static final int MAX_COMMAND_LENGTH = 1024;
    private static final int MAX_BYTES = 2 * 1024 * 1024;

    private MpvScripts() {
    }

    // SharedPreferences provides an atomic snapshot; readers never wait for file I/O.
    public static List<Item> read() throws JSONException {
        JSONArray array = new JSONArray(Prefers.getString(PREFERENCE_KEY, "[]"));
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            String id = object.getString("id");
            if (!id.matches("[a-f0-9]{32}")) throw new JSONException("Invalid script ID");
            String name = object.getString("fileName");
            String command = object.optString("command", "");
            String legacyBinding = object.optString("binding", "");
            boolean automatic = object.getBoolean("automatic");
            if (!legacyBinding.isEmpty()) {
                if (!command.isEmpty()) throw new JSONException("Invalid command button");
                command = "script-binding " + validateBinding(legacyBinding);
            }
            if (!command.isEmpty()) {
                command = validateCommand(command);
                if (automatic || !name.isEmpty()) throw new JSONException("Invalid command button");
            } else if (!name.equals(new File(name).getName()) || name.contains("\\") || !name.endsWith(".lua")) throw new JSONException("Invalid script filename");
            items.add(new Item(id, validateTitle(object.getString("title")), name, automatic, object.getBoolean("enabled"), command, object.optBoolean("hidden", false)));
        }
        return items;
    }

    private static void write(List<Item> items) throws JSONException {
        JSONArray array = new JSONArray();
        for (Item item : items) array.put(new JSONObject().put("id", item.id).put("title", item.title).put("fileName", item.fileName).put("automatic", item.automatic).put("enabled", item.enabled).put("command", item.command).put("hidden", item.hidden));
        Prefers.put(PREFERENCE_KEY, array.toString());
    }

    public static Item importFrom(Uri uri, boolean automatic, String replaceId) throws IOException, JSONException {
        String name = FileUtil.getDisplayName(uri, "script.lua");
        if (!name.toLowerCase(Locale.ROOT).endsWith(".lua")) throw new IOException("Please select a .lua file");
        name = name.substring(0, name.length() - 4).replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_") + ".lua";
        String source;
        try (InputStream input = App.get().getContentResolver().openInputStream(uri)) {
            source = readText(input);
        }
        return install(name, source, automatic, replaceId);
    }

    private static String readText(InputStream input) throws IOException {
        if (input == null) throw new IOException("Unable to open Lua file");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (output.size() + count > MAX_BYTES) throw new IOException("Lua file exceeds 2 MiB");
            output.write(buffer, 0, count);
        }
        String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(output.toByteArray())).toString();
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    private static synchronized Item install(String name, String source, boolean automatic, String replaceId) throws IOException, JSONException {
        List<Item> items = read();
        boolean added = replaceId == null;
        Item item = added ? createItem(name, automatic) : items.get(requireIndex(items, replaceId));
        if (item.isCommand()) throw new JSONException("A command button has no Lua file");
        try {
            if (!added) migrate(item);
            FileUtil.writeAtomically(source.getBytes(StandardCharsets.UTF_8), item.source());
            if (added) items.add(item);
            write(items);
        } catch (IOException | JSONException e) {
            if (added) {
                try {
                    deleteFiles(item);
                } catch (IOException cleanup) {
                    e.addSuppressed(cleanup);
                }
            }
            throw e;
        }
        return item;
    }

    private static Item createItem(String name, boolean automatic) throws JSONException {
        String title = name.substring(0, name.length() - 4);
        int end = Math.min(title.length(), MAX_TITLE_LENGTH);
        if (end > 0 && Character.isHighSurrogate(title.charAt(end - 1))) end--;
        return new Item(UUID.randomUUID().toString().replace("-", ""), validateTitle(title.substring(0, end)), name, automatic, true);
    }

    static synchronized File prepareRunner(Item item, String generation) throws IOException {
        migrate(item);
        if (item.isCommand()) return writeCommandRunner(item, generation);
        return writeRunner(item, "script-button.lua", item.runner(), generation);
    }

    static synchronized File prepareStartup(Item item, String generation) throws IOException {
        migrate(item);
        return writeRunner(item, "script-startup.lua", item.startup(), generation);
    }

    private static void migrate(Item item) throws IOException {
        if (item.isCommand()) return;
        File directory = Path.files("mpv-scripts/" + item.id);
        if (directory.exists()) {
            migrateDirectory(directory, item.source().getParentFile(), item.automatic ? null : "tv_" + item.id + ".lua");
        }
        if (item.automatic) {
            File startup = Path.cache("mpv-scripts/" + item.id);
            if (startup.exists()) {
                migrateDirectory(startup, item.startup().getParentFile(), item.fileName);
            }
        }
        deleteEmptyDirectory(Path.files("mpv-scripts"));
        deleteEmptyDirectory(Path.cache("mpv-scripts"));
    }

    private static void migrateDirectory(File source, File target, String generatedName) throws IOException {
        if (!source.getCanonicalFile().equals(new File(source.getParentFile().getCanonicalFile(), source.getName()))) throw new IOException("Unable to move a script symbolic link");
        File[] files = source.listFiles();
        if (files == null) throw new IOException("Unable to read script directory");
        if (!target.isDirectory() && !target.mkdirs() && !target.isDirectory()) throw new IOException("Unable to create script directory");
        for (File file : files) {
            if (file.getName().equals(generatedName)) continue;
            if (!file.getCanonicalFile().equals(new File(source.getCanonicalFile(), file.getName()))) throw new IOException("Unable to move a script symbolic link");
            File destination = new File(target, file.getName());
            if (file.isDirectory()) migrateDirectory(file, destination, null);
            else {
                if (!destination.exists()) FileUtil.copyAtomically(file, destination);
                else if (!Arrays.equals(Crypto.sha256(file), Crypto.sha256(destination))) throw new IOException("Different script file already exists: " + file.getName());
                deleteFile(file);
            }
        }
        // Generated wrappers contain old absolute paths and are recreated after migration.
        if (generatedName != null) deleteFile(new File(source, generatedName));
        deleteEmptyDirectory(source);
    }

    private static File writeRunner(Item item, String asset, File file, String generation) throws IOException {
        String header = "local source = " + quote(item.source().getAbsolutePath()) + "\nlocal directory = " + quote(item.source().getParentFile().getAbsolutePath()) + "\nlocal status = " + quote("user-data/" + item.statusKey()) + "\n";
        return writeRunner(asset, file, header + "local generation = " + quote(generation) + "\n");
    }

    private static File writeCommandRunner(Item item, String generation) throws IOException {
        String header = "local command = " + quote(item.command) + "\nlocal status = " + quote("user-data/" + item.statusKey()) + "\nlocal generation = " + quote(generation) + "\n";
        return writeRunner("command-button.lua", item.runner(), header);
    }

    private static File writeRunner(String asset, File file, String header) throws IOException {
        try (InputStream input = App.get().getAssets().open("mpv/" + asset)) {
            FileUtil.writeAtomically((header + readText(input)).getBytes(StandardCharsets.UTF_8), file);
        }
        return file;
    }

    private static String quote(String value) {
        String equals = "";
        while (value.contains("]" + equals + "]")) equals += "=";
        return "[" + equals + "[" + value + "]" + equals + "]";
    }

    private static String validateTitle(String title) throws JSONException {
        title = title.strip();
        if (title.isEmpty() || title.length() > MAX_TITLE_LENGTH || title.indexOf('\0') >= 0) throw new JSONException("Invalid title");
        return title;
    }

    private static String validateBinding(String binding) throws JSONException {
        binding = binding.strip();
        int slash = binding.indexOf('/');
        if (slash <= 0 || slash == binding.length() - 1 || binding.length() > 256 || binding.chars().anyMatch(Character::isISOControl)) throw new JSONException("Use script-name/action-name");
        return binding;
    }

    private static String validateCommand(String command) throws JSONException {
        command = command.strip();
        if (command.isEmpty() || command.length() > MAX_COMMAND_LENGTH || command.chars().anyMatch(Character::isISOControl)) throw new JSONException("Invalid MPV command");
        return command;
    }

    public static synchronized Item addCommand(String title, String command) throws JSONException {
        List<Item> items = read();
        Item item = new Item(UUID.randomUUID().toString().replace("-", ""), validateTitle(title), "", false, true, validateCommand(command), false);
        items.add(item);
        write(items);
        return item;
    }

    public static synchronized void updateCommand(String id, String title, String command) throws JSONException {
        String name = validateTitle(title);
        String value = validateCommand(command);
        List<Item> items = read();
        int index = requireIndex(items, id);
        Item item = items.get(index);
        if (!item.isCommand()) throw new JSONException("Item is not a command button");
        items.set(index, new Item(item.id, name, "", false, item.enabled, value, item.hidden));
        write(items);
    }

    private static int requireIndex(List<Item> items, String id) throws JSONException {
        for (int i = 0; i < items.size(); i++) if (items.get(i).id.equals(id)) return i;
        throw new JSONException("Script was deleted");
    }

    public static void rename(String id, String title) throws JSONException {
        String name = validateTitle(title);
        update(id, item -> new Item(item.id, name, item.fileName, item.automatic, item.enabled, item.command, item.hidden));
    }

    public static void setEnabled(String id, boolean enabled) throws JSONException {
        update(id, item -> new Item(item.id, item.title, item.fileName, item.automatic, enabled, item.command, item.hidden));
    }

    public static void setHidden(String id, boolean hidden) throws JSONException {
        update(id, item -> new Item(item.id, item.title, item.fileName, item.automatic, item.enabled, item.command, hidden));
    }

    /** Moves a button past its neighboring button while preserving startup-script order. */
    public static synchronized void move(String id, boolean earlier) throws JSONException {
        List<Item> items = read();
        int index = requireIndex(items, id);
        if (items.get(index).automatic) throw new JSONException("Only action buttons can be moved");
        int step = earlier ? -1 : 1;
        for (int target = index + step; target >= 0 && target < items.size(); target += step) {
            if (items.get(target).automatic) continue;
            Collections.swap(items, index, target);
            write(items);
            return;
        }
    }

    private static synchronized void update(String id, UnaryOperator<Item> operation) throws JSONException {
        List<Item> items = read();
        int index = requireIndex(items, id);
        items.set(index, operation.apply(items.get(index)));
        write(items);
    }

    public static synchronized void delete(String id) throws IOException, JSONException {
        List<Item> items = read();
        int index = requireIndex(items, id);
        deleteFiles(items.get(index));
        items.remove(index);
        write(items);
    }

    private static void deleteFiles(Item item) throws IOException {
        if (item.isCommand()) {
            deleteFile(item.runner());
            deleteEmptyDirectory(item.runner().getParentFile());
            return;
        }
        deleteFile(item.source());
        if (!item.automatic) deleteFile(item.runner());
        else {
            File startup = item.startup();
            deleteFile(startup);
            deleteEmptyDirectory(startup.getParentFile());
        }
        deleteEmptyDirectory(item.source().getParentFile());
        File legacy = Path.files("mpv-scripts/" + item.id);
        deleteFile(new File(legacy, item.fileName));
        if (!item.automatic) deleteFile(new File(legacy, "tv_" + item.id + ".lua"));
        else {
            File startup = new File(Path.cache("mpv-scripts/" + item.id), item.fileName);
            deleteFile(startup);
            deleteEmptyDirectory(startup.getParentFile());
        }
        deleteEmptyDirectory(legacy);
    }

    private static void deleteEmptyDirectory(File directory) throws IOException {
        String[] remaining = directory.list();
        // Scripts may create their own data files. Only remove an empty directory.
        if (remaining != null && remaining.length == 0 && !directory.delete()) throw new IOException("Unable to delete script directory");
    }

    private static void deleteFile(File file) throws IOException {
        if (file.exists() && !file.delete()) throw new IOException("Unable to delete " + file.getName());
    }

    public static final class Item {
        public final String id;
        public final String title;
        public final String fileName;
        public final boolean automatic;
        public final boolean enabled;
        public final String command;
        public final boolean hidden;

        Item(String id, String title, String fileName, boolean automatic, boolean enabled) {
            this(id, title, fileName, automatic, enabled, "", false);
        }

        private Item(String id, String title, String fileName, boolean automatic, boolean enabled, String command, boolean hidden) {
            this.id = id;
            this.title = title;
            this.fileName = fileName;
            this.automatic = automatic;
            this.enabled = enabled;
            this.command = command;
            this.hidden = hidden;
        }

        public boolean isCommand() {
            return !command.isEmpty();
        }

        File source() {
            return new File(Path.mpv("lua/" + id), fileName);
        }

        File runner() {
            return isCommand() ? new File(Path.cache("mpv-command-buttons"), "tv_" + id + ".lua") : new File(source().getParentFile(), "tv_" + id + ".lua");
        }

        File startup() {
            return new File(Path.mpv("lua/.startup/" + id), fileName);
        }

        String statusKey() {
            return "tv-scripts/" + id;
        }

        String binding() {
            return "tv_" + id + "/run";
        }
    }
}
