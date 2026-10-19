package com.fongmi.android.tv.server;

import android.util.Base64;

import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Path;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;

public final class SpiderSource {

    public static final int MAX_BYTES = 2 * 1024 * 1024;
    private final File file;
    private final File history;
    private boolean bom;
    private boolean crlf;
    private boolean backedUp;
    private byte[] data;
    private String version;
    private String text;

    public SpiderSource(File file) throws Exception {
        this.file = file;
        this.history = new File(Path.cache(), "spider-source-history/" + Crypto.md5(file.getCanonicalPath()));
        update(read(file));
    }

    private static byte[] read(File file) throws IOException {
        byte[] bytes;
        try (FileInputStream input = new FileInputStream(file); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > MAX_BYTES) throw new IOException("Source exceeds 2 MiB");
                output.write(buffer, 0, count);
            }
            bytes = output.toByteArray();
        }
        return bytes;
    }

    private void update(byte[] bytes) throws Exception {
        String value = decode(bytes);
        bom = value.startsWith("\uFEFF");
        crlf = value.contains("\r\n");
        text = (bom ? value.substring(1) : value).replace("\r\n", "\n");
        version = fingerprint(bytes);
        data = bytes;
    }

    public synchronized JSONObject snapshot() throws Exception {
        return new JSONObject().put("name", file.getName()).put("path", file.getAbsolutePath()).put("text", text).put("version", version).put("writable", file.canWrite());
    }

    public synchronized JSONObject save(String expected, String value) throws Exception {
        requireVersion(expected);
        if (!file.canWrite()) throw new SecurityException("File is read-only");
        value = value.replace("\r\n", "\n");
        byte[] bytes = ((bom ? "\uFEFF" : "") + (crlf ? value.replace("\n", "\r\n") : value)).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new IOException("Source exceeds 2 MiB");
        if (!backedUp) {
            checkpoint();
            backedUp = true;
        }
        Path.writeAtomically(file, bytes);
        text = value;
        version = fingerprint(bytes);
        data = bytes;
        return new JSONObject().put("version", version);
    }

    public synchronized JSONArray checkpoint() throws Exception {
        requireVersion(version);
        File backup = new File(history, id(version) + ".bak");
        if (!backup.isFile()) Path.writeAtomically(backup, data);
        File[] files = backups();
        Arrays.sort(files, Comparator.comparing((File file) -> !file.equals(backup)).thenComparing(Comparator.comparingLong(File::lastModified).reversed()).thenComparing(File::getName));
        for (int index = 8; index < files.length; index++) if (!files[index].delete()) throw new IOException("Cannot prune source version");
        return versions();
    }

    public synchronized JSONArray versions() throws Exception {
        JSONArray result = new JSONArray();
        for (File backup : backups()) result.put(new JSONObject().put("id", backup.getName().substring(0, 43)).put("time", backup.lastModified()).put("size", backup.length()).put("current", backup.getName().equals(id(version) + ".bak")));
        return result;
    }

    public synchronized JSONObject version(String id) throws Exception {
        byte[] bytes = readVersion(id);
        String text = decode(bytes);
        return new JSONObject().put("id", id).put("text", (text.startsWith("\uFEFF") ? text.substring(1) : text).replace("\r\n", "\n"));
    }

    public synchronized JSONObject restore(String path, String expected, String id) throws Exception {
        if (!file.getAbsolutePath().equals(path)) throw new ConcurrentModificationException("Selected source changed. Reopen the file before restoring.");
        requireVersion(expected);
        if (!file.canWrite()) throw new SecurityException("File is read-only");
        byte[] bytes = readVersion(id);
        checkpoint();
        requireVersion(expected);
        Path.writeAtomically(file, bytes);
        update(bytes);
        backedUp = false;
        return snapshot();
    }

    private byte[] readVersion(String id) throws Exception {
        byte[] bytes = read(backup(id));
        if (!id(fingerprint(bytes)).equals(id)) throw new IOException("Source version is corrupt");
        return bytes;
    }

    private static String decode(byte[] bytes) throws IOException {
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }

    private File backup(String id) throws IOException {
        if (!id.matches("[A-Za-z0-9_-]{43}")) throw new IllegalArgumentException("Invalid source version");
        File backup = new File(history, id + ".bak");
        if (!backup.isFile()) throw new IOException("Source version is missing");
        return backup;
    }

    private File[] backups() throws IOException {
        if (!history.exists()) return new File[0];
        File[] files = history.listFiles(file -> file.isFile() && file.getName().matches("[A-Za-z0-9_-]{43}\\.bak"));
        if (files == null) throw new IOException("Cannot read source versions");
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed().thenComparing(File::getName));
        return files;
    }

    private void requireVersion(String expected) throws IOException {
        if (!version.equals(expected) || !version.equals(fingerprint())) throw new ConcurrentModificationException("Source changed. Reopen the file before saving.");
    }

    private static String id(String version) {
        return version.replace('+', '-').replace('/', '_').replace("=", "");
    }

    private String fingerprint() throws IOException {
        return Base64.encodeToString(Crypto.sha256(file), Base64.NO_WRAP);
    }

    private static String fingerprint(byte[] bytes) throws Exception {
        return Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes), Base64.NO_WRAP);
    }
}
