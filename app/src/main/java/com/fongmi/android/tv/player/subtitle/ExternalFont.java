package com.fongmi.android.tv.player.subtitle;

import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.LruCache;

import androidx.annotation.Nullable;
import androidx.media3.common.text.SubtitleFontFile;
import androidx.media3.exoplayer.libass.LibassFontFile;

import com.fongmi.android.tv.utils.FileUtil;
import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Path;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ExternalFont {

    private static final String MPV_FONT_PREFIX = "mpv://";
    private static final String SUBTITLE_FILE_PREFIX = "font:";
    private static final String TEMP_FILE_PREFIX = ".external-font-";
    private static final String TEMP_FILE_SUFFIX = ".tmp";
    private static final int COPY_BUFFER_SIZE = 64 * 1024;
    private static final long MAX_FILE_BYTES = 32L * 1024 * 1024;
    private static final int TYPEFACE_CACHE_SIZE = 12;
    private static final int MAX_FILE_NAME_CODE_POINTS = 48;
    private static final Object CACHE_LOCK = new Object();
    private static final Object IMPORT_LOCK = new Object();
    private static final Map<String, CachedItem> ITEM_CACHE = new HashMap<>();
    private static final LruCache<String, Typeface> TYPEFACE_CACHE = new LruCache<>(TYPEFACE_CACHE_SIZE);

    public static File getDirectory() {
        return Path.font();
    }

    public static File getMpvDirectory() {
        return Path.mpv("fonts");
    }

    public static List<Entry> getAll() {
        return getAll(null);
    }

    public static List<Entry> getAll(@Nullable File additionalDirectory) {
        return getAll(additionalDirectory, null, false);
    }

    public static List<Entry> getSubtitleEntries(@Nullable File additionalDirectory, @Nullable Item selected) {
        return getAll(additionalDirectory, selected, true);
    }

    private static List<Entry> getAll(@Nullable File additionalDirectory, @Nullable Item selected, boolean includeCollectionFaces) {
        List<File> directories = getDirectories(additionalDirectory);
        List<File[]> filesByDirectory = new ArrayList<>();
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < directories.size(); i++) {
            File[] files = listSupportedFiles(directories.get(i));
            if (files != null) filesByDirectory.add(files);
            addEntries(entries, files, i != 0, includeCollectionFaces);
        }
        if (selected != null && entries.stream().noneMatch(entry -> entry.item().sameFace(selected))) {
            File file = selected.file();
            filesByDirectory.add(new File[]{file});
            entries.addAll(getEntries(file, selected.source() != null, includeCollectionFaces));
        }
        pruneCache(filesByDirectory);
        return entries;
    }

    static List<File> getDirectories(@Nullable File additionalDirectory) {
        List<File> directories = new ArrayList<>();
        directories.add(getDirectory());
        addDirectory(directories, getMpvDirectory());
        if (additionalDirectory != null) addDirectory(directories, additionalDirectory);
        return directories;
    }

    private static void addDirectory(List<File> directories, File directory) {
        for (File existing : directories) if (sameDirectory(existing, directory)) return;
        directories.add(directory);
    }

    private static void addEntries(List<Entry> entries, @Nullable File[] files, boolean additional, boolean includeCollectionFaces) {
        if (files == null) return;
        Arrays.stream(files)
                .sorted((first, second) -> first.getName().compareToIgnoreCase(second.getName()))
                .flatMap(file -> getEntries(file, additional, includeCollectionFaces).stream())
                .forEach(entries::add);
    }

    @Nullable
    public static Item find(String fileName) {
        File file = getFile(fileName);
        return file == null ? null : getItem(file);
    }

    @Nullable
    public static Item find(String key, @Nullable File additionalDirectory) {
        if (!key.startsWith(MPV_FONT_PREFIX)) return find(key);
        File file = new File(key.substring(MPV_FONT_PREFIX.length()));
        if (!file.isAbsolute() || !isSupportedFile(file)) return null;
        File directory = file.getParentFile();
        if (sameDirectory(directory, getDirectory())) return find(file.getName());
        if (sameDirectory(directory, getMpvDirectory())) return getItem(file, true);
        if (additionalDirectory == null || !sameDirectory(directory, additionalDirectory)) return null;
        return getItem(file, true);
    }

    @Nullable
    public static Item findSubtitle(String key) {
        if (!key.startsWith(SUBTITLE_FILE_PREFIX)) return find(key, null);
        int separator = key.indexOf(':', SUBTITLE_FILE_PREFIX.length());
        if (separator < 0) return null;
        try {
            int index = Integer.parseInt(key.substring(SUBTITLE_FILE_PREFIX.length(), separator));
            File file = new File(key.substring(separator + 1));
            if (!file.isAbsolute() || !isSupportedFile(file)) return null;
            boolean additional = !sameDirectory(file.getParentFile(), getDirectory());
            return getItem(file, additional, index);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static List<Entry> readSubtitleFile(File file) throws IOException {
        if (!isSupportedFile(file)) throw new IOException("Unsupported font file");
        List<Entry> entries = getEntries(file, !sameDirectory(file.getParentFile(), getDirectory()), true);
        if (entries.isEmpty()) throw new IOException("Unsupported font face");
        return entries;
    }

    @Nullable
    public static Typeface getTypeface(@Nullable Item item) {
        synchronized (CACHE_LOCK) {
            return item == null ? null : getTypefaceLocked(item);
        }
    }

    public static Item importFrom(Uri uri) throws IOException {
        File directory = getDirectory();
        File temporary = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX, directory);
        try {
            PreparedFont font = prepareFont(uri, temporary);
            return installFont(uri, directory, temporary, font);
        } finally {
            Path.clear(temporary);
        }
    }

    @Nullable
    private static Typeface getTypefaceLocked(Item item) {
        String cacheKey = item.cacheKey();
        Typeface typeface = TYPEFACE_CACHE.get(cacheKey);
        return typeface == null ? loadTypeface(item, cacheKey) : typeface;
    }

    @Nullable
    private static Typeface loadTypeface(Item item, String cacheKey) {
        File file = item.file();
        if (!isSupportedFile(file)) return null;
        Typeface typeface = createTypeface(file, item.faceIndex());
        if (typeface != null) TYPEFACE_CACHE.put(cacheKey, typeface);
        return typeface;
    }

    private static List<Entry> getEntries(File file, boolean additional, boolean includeCollectionFaces) {
        synchronized (CACHE_LOCK) {
            List<Entry> result = new ArrayList<>();
            for (Item item : getItemsLocked(file, additional)) {
                if (!includeCollectionFaces && item.faceIndex() != 0) continue;
                Typeface typeface = getTypefaceLocked(item);
                if (typeface != null) result.add(new Entry(item, typeface));
            }
            return result;
        }
    }

    private static boolean sameDirectory(File first, File second) {
        try {
            return first.getCanonicalFile().equals(second.getCanonicalFile());
        } catch (IOException | SecurityException e) {
            return first.getAbsoluteFile().equals(second.getAbsoluteFile());
        }
    }

    private static PreparedFont prepareFont(Uri uri, File temporary) throws IOException {
        byte[] sha256 = copyWithSha256(uri, temporary);
        if (createTypeface(temporary, 0) == null) throw new IOException("Unsupported font file");
        return new PreparedFont(sha256, requireFamilyName(temporary));
    }

    private static byte[] copyWithSha256(Uri uri, File target) throws IOException {
        MessageDigest digest = Crypto.newDigest("SHA-256");
        try (InputStream input = Path.open(uri, "Unable to open source file"); FileOutputStream fileOutput = new FileOutputStream(target); DigestOutputStream output = new DigestOutputStream(fileOutput, digest)) {
            copyBytes(input, output);
            output.flush();
            fileOutput.getFD().sync();
        }
        return digest.digest();
    }

    private static void copyBytes(InputStream input, DigestOutputStream output) throws IOException {
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        long totalBytes = 0L;
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Canceled");
            totalBytes += count;
            if (totalBytes > MAX_FILE_BYTES) throw new IOException("File is too large");
            output.write(buffer, 0, count);
        }
    }

    private static Item installFont(Uri uri, File directory, File temporary, PreparedFont font) throws IOException {
        synchronized (IMPORT_LOCK) {
            Item duplicate = findDuplicate(directory, font.sha256());
            return duplicate == null ? installNewFont(uri, directory, temporary, font) : duplicate;
        }
    }

    private static Item installNewFont(Uri uri, File directory, File temporary, PreparedFont font) throws IOException {
        String displayName = FileUtil.getDisplayName(uri, "font.ttf");
        File target = nextAvailableFile(directory, sanitizeFileName(displayName));
        Path.move(temporary, target);
        return new Item(target.getName(), font.familyName(), getRevision(target));
    }

    private static String requireFamilyName(File file) throws IOException {
        String familyName = LibassFontFile.getFamilyName(file);
        if (TextUtils.isEmpty(familyName)) throw new IOException("Font family name is missing");
        return familyName;
    }

    @Nullable
    private static Item getItem(File file) {
        return getItem(file, false);
    }

    @Nullable
    private static Item getItem(File file, boolean additional) {
        return getItem(file, additional, 0);
    }

    @Nullable
    private static Item getItem(File file, boolean additional, int index) {
        synchronized (CACHE_LOCK) {
            for (Item item : getItemsLocked(file, additional)) {
                if (item.faceIndex() == index) return getTypefaceLocked(item) == null ? null : item;
            }
            return null;
        }
    }

    private static List<Item> getItemsLocked(File file, boolean additional) {
        String path = file.getAbsolutePath();
        long length = file.length();
        long modified = file.lastModified();
        CachedItem cached = ITEM_CACHE.get(path);
        if (cached != null && cached.matches(length, modified, additional)) return cached.items();
        removeTypeface(cached);
        List<Item> items = readItems(file, additional);
        ITEM_CACHE.put(path, new CachedItem(items, length, modified, additional));
        return items;
    }

    private static List<Item> readItems(File file, boolean additional) {
        try {
            List<Item> items = new ArrayList<>();
            long revision = getRevision(file);
            File source = additional ? file.getAbsoluteFile() : null;
            for (LibassFontFile.Face face : LibassFontFile.listFaces(file)) {
                if (face.index != 0 && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) continue;
                String label = face.inCollection ? getFaceLabel(face) : file.getName();
                items.add(new Item(file.getName(), face.familyName, revision, source, face.index, label));
            }
            return List.copyOf(items);
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
    }

    private static String getFaceLabel(LibassFontFile.Face face) {
        return face.styleName == null ? face.familyName + " " + (face.index + 1) : face.familyName + " " + face.styleName;
    }

    private static void removeTypeface(@Nullable CachedItem cached) {
        if (cached == null) return;
        for (Item item : cached.items()) TYPEFACE_CACHE.remove(item.cacheKey());
    }

    private static void pruneCache(List<File[]> filesByDirectory) {
        if (filesByDirectory.isEmpty()) return;
        Set<String> paths = filesByDirectory.stream().flatMap(Arrays::stream).map(File::getAbsolutePath).collect(Collectors.toSet());
        synchronized (CACHE_LOCK) {
            ITEM_CACHE.entrySet().removeIf(entry -> removeMissingEntry(paths, entry));
        }
    }

    private static boolean removeMissingEntry(Set<String> paths, Map.Entry<String, CachedItem> entry) {
        boolean missing = !paths.contains(entry.getKey());
        if (missing) removeTypeface(entry.getValue());
        return missing;
    }

    private static long getRevision(File file) {
        long revision = 31L * file.getName().hashCode() + file.length();
        return 31L * revision + file.lastModified();
    }

    @Nullable
    private static File[] listSupportedFiles(File directory) {
        try {
            return directory.listFiles(ExternalFont::isSupportedFile);
        } catch (SecurityException e) {
            return null;
        }
    }

    @Nullable
    private static Item findDuplicate(File directory, byte[] sha256) {
        File[] files = listSupportedFiles(directory);
        if (files == null) return null;
        return Arrays.stream(files)
                .filter(file -> hasSha256(file, sha256))
                .map(ExternalFont::getItem)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static boolean hasSha256(File file, byte[] expected) {
        try {
            return Arrays.equals(expected, Crypto.sha256(file));
        } catch (IOException e) {
            return false;
        }
    }

    private static File nextAvailableFile(File directory, String fileName) {
        File target = new File(directory, fileName);
        if (!target.exists()) return target;
        int extensionStart = fileName.lastIndexOf('.');
        String baseName = fileName.substring(0, extensionStart);
        String extension = fileName.substring(extensionStart);
        int suffix = 2;
        while (target.exists()) target = new File(directory, baseName + " (" + suffix++ + ")" + extension);
        return target;
    }

    private static String sanitizeFileName(String displayName) {
        String name = getFileName(displayName);
        String extension = getExtension(name);
        String stem = getStem(name, extension);
        String sanitized = sanitizeStem(stem).trim();
        return (sanitized.isEmpty() ? "font" : sanitized) + extension;
    }

    private static String getFileName(String displayName) {
        int slash = Math.max(displayName.lastIndexOf('/'), displayName.lastIndexOf('\\'));
        return displayName.substring(slash + 1);
    }

    private static String getStem(String fileName, String extension) {
        int extensionStart = fileName.toLowerCase(Locale.US).lastIndexOf(extension);
        return extensionStart > 0 ? fileName.substring(0, extensionStart) : fileName;
    }

    private static String sanitizeStem(String stem) {
        return stem.codePoints()
                .limit(MAX_FILE_NAME_CODE_POINTS)
                .map(ExternalFont::sanitizeCodePoint)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private static int sanitizeCodePoint(int codePoint) {
        boolean invalid = Character.isISOControl(codePoint) || "<>:\"/\\|?*".indexOf(codePoint) >= 0;
        return invalid ? '_' : codePoint;
    }

    private static String getExtension(String displayName) {
        String name = displayName.toLowerCase(Locale.US);
        if (name.endsWith(".otf")) return ".otf";
        if (name.endsWith(".ttc")) return ".ttc";
        if (name.endsWith(".otc")) return ".otc";
        return ".ttf";
    }

    @Nullable
    private static File getFile(String fileName) {
        if (TextUtils.isEmpty(fileName) || !fileName.equals(new File(fileName).getName())) return null;
        File file = new File(getDirectory(), fileName);
        return isSupportedFile(file) ? file : null;
    }

    private static boolean isSupportedFile(File file) {
        if (file == null || !file.isFile() || !file.canRead() || file.length() <= 0 || file.length() > MAX_FILE_BYTES) return false;
        String name = file.getName().toLowerCase(Locale.US);
        return name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".ttc") || name.endsWith(".otc");
    }

    @Nullable
    private static Typeface createTypeface(File file, int faceIndex) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return new Typeface.Builder(file).setTtcIndex(faceIndex).build();
            return faceIndex == 0 ? Typeface.createFromFile(file) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public record Item(String fileName, String familyName, long revision, @Nullable File source, int faceIndex, String displayName) {

        public Item(String fileName, String familyName, long revision) {
            this(fileName, familyName, revision, null);
        }

        public Item(String fileName, String familyName, long revision, @Nullable File source) {
            this(fileName, familyName, revision, source, 0, fileName);
        }

        public String storageKey() {
            return source == null ? fileName : MPV_FONT_PREFIX + source.getAbsolutePath();
        }

        public String subtitleStorageKey() {
            return SUBTITLE_FILE_PREFIX + faceIndex + ':' + file().getAbsolutePath();
        }

        public File file() {
            return source == null ? new File(getDirectory(), fileName) : source;
        }

        public File directory() {
            return file().getParentFile();
        }

        public boolean sameFace(Item other) {
            return faceIndex == other.faceIndex && file().equals(other.file());
        }

        public SubtitleFontFile subtitleFontFile() {
            return new SubtitleFontFile(file(), faceIndex);
        }

        private String cacheKey() {
            return subtitleStorageKey() + ':' + revision;
        }
    }

    public record Entry(Item item, @Nullable Typeface typeface) {
    }

    private record PreparedFont(byte[] sha256, String familyName) {
    }

    private record CachedItem(List<Item> items, long length, long modified, boolean additional) {
        private boolean matches(long length, long modified, boolean additional) {
            return this.length == length && this.modified == modified && this.additional == additional;
        }
    }
}
