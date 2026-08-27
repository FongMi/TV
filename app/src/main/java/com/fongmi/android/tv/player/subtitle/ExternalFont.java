package com.fongmi.android.tv.player.subtitle;

import android.graphics.Typeface;
import android.net.Uri;
import android.text.TextUtils;
import android.util.LruCache;

import androidx.annotation.Nullable;
import androidx.media3.exoplayer.libass.LibassFontFile;

import com.fongmi.android.tv.App;
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

    public static List<Entry> getAll() {
        File[] files = listSupportedFiles(getDirectory());
        if (files == null) return List.of();
        pruneCache(files);
        return Arrays.stream(files)
                .sorted((first, second) -> first.getName().compareToIgnoreCase(second.getName()))
                .map(ExternalFont::getEntry)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Nullable
    public static Item find(String fileName) {
        File file = getFile(fileName);
        return file == null ? null : getItem(file);
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
        File file = getFile(item.fileName());
        Typeface typeface = file == null ? null : createTypeface(file);
        if (typeface != null) TYPEFACE_CACHE.put(cacheKey, typeface);
        return typeface;
    }

    @Nullable
    private static Entry getEntry(File file) {
        synchronized (CACHE_LOCK) {
            Item item = getItemLocked(file);
            return item == null ? null : new Entry(item, getTypefaceLocked(item));
        }
    }

    private static PreparedFont prepareFont(Uri uri, File temporary) throws IOException {
        byte[] sha256 = copyWithSha256(uri, temporary);
        Typeface typeface = createTypefaceOrThrow(temporary);
        return new PreparedFont(sha256, typeface, requireFamilyName(temporary));
    }

    private static byte[] copyWithSha256(Uri uri, File target) throws IOException {
        MessageDigest digest = Crypto.newDigest("SHA-256");
        try (InputStream input = openInputStream(uri); FileOutputStream fileOutput = new FileOutputStream(target); DigestOutputStream output = new DigestOutputStream(fileOutput, digest)) {
            byte[] buffer = new byte[COPY_BUFFER_SIZE];
            long totalBytes = 0L;
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Canceled");
                totalBytes += count;
                if (totalBytes > MAX_FILE_BYTES) throw new IOException("File is too large");
                output.write(buffer, 0, count);
            }
            output.flush();
            fileOutput.getFD().sync();
        }
        return digest.digest();
    }

    private static InputStream openInputStream(Uri uri) throws IOException {
        InputStream input = App.get().getContentResolver().openInputStream(uri);
        if (input == null) throw new IOException("Unable to open source file");
        return input;
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
        Item item = createItem(target, font.familyName());
        cacheItem(target, item, font.typeface());
        return item;
    }

    private static String requireFamilyName(File file) throws IOException {
        String familyName = LibassFontFile.getFamilyName(file);
        if (TextUtils.isEmpty(familyName)) throw new IOException("Font family name is missing");
        return familyName;
    }

    private static void cacheItem(File file, Item item, Typeface typeface) {
        synchronized (CACHE_LOCK) {
            ITEM_CACHE.put(file.getAbsolutePath(), new CachedItem(item, file.length(), file.lastModified()));
            TYPEFACE_CACHE.put(item.cacheKey(), typeface);
        }
    }

    @Nullable
    private static Item getItem(File file) {
        synchronized (CACHE_LOCK) {
            return getItemLocked(file);
        }
    }

    @Nullable
    private static Item getItemLocked(File file) {
        String path = file.getAbsolutePath();
        long length = file.length();
        long modified = file.lastModified();
        CachedItem cached = ITEM_CACHE.get(path);
        if (cached == null || !cached.matches(length, modified)) cached = refreshItem(file, cached, length, modified);
        return cached.item();
    }

    private static CachedItem refreshItem(File file, @Nullable CachedItem cached, long length, long modified) {
        removeTypeface(cached);
        Typeface typeface = createTypeface(file);
        String familyName = typeface == null ? null : readFamilyName(file);
        Item item = TextUtils.isEmpty(familyName) ? null : createItem(file, familyName);
        CachedItem current = new CachedItem(item, length, modified);
        ITEM_CACHE.put(file.getAbsolutePath(), current);
        if (item != null) TYPEFACE_CACHE.put(item.cacheKey(), typeface);
        return current;
    }

    private static void removeTypeface(@Nullable CachedItem cached) {
        if (cached != null && cached.item() != null) TYPEFACE_CACHE.remove(cached.item().cacheKey());
    }

    private static Item createItem(File file, String familyName) {
        return new Item(file.getName(), familyName, getRevision(file));
    }

    private static void pruneCache(File[] files) {
        Set<String> paths = Arrays.stream(files).map(File::getAbsolutePath).collect(Collectors.toSet());
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
        return directory.listFiles(ExternalFont::isSupportedFile);
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
        return target.exists() ? findAvailableFile(directory, fileName) : target;
    }

    private static File findAvailableFile(File directory, String fileName) {
        int extensionStart = fileName.lastIndexOf('.');
        String baseName = fileName.substring(0, extensionStart);
        String extension = fileName.substring(extensionStart);
        int suffix = 2;
        File target = getSuffixedFile(directory, baseName, extension, suffix);
        while (target.exists()) target = getSuffixedFile(directory, baseName, extension, ++suffix);
        return target;
    }

    private static File getSuffixedFile(File directory, String baseName, String extension, int suffix) {
        return new File(directory, baseName + " (" + suffix + ")" + extension);
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
        return ".ttf";
    }

    @Nullable
    private static File getFile(String fileName) {
        if (TextUtils.isEmpty(fileName) || !fileName.equals(new File(fileName).getName())) return null;
        File file = new File(getDirectory(), fileName);
        return isSupportedFile(file) ? file : null;
    }

    private static boolean isSupportedFile(File file) {
        if (file == null || !file.isFile() || file.length() <= 0 || file.length() > MAX_FILE_BYTES) return false;
        String name = file.getName().toLowerCase(Locale.US);
        return name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".ttc");
    }

    private static Typeface createTypefaceOrThrow(File file) throws IOException {
        Typeface typeface = createTypeface(file);
        if (typeface == null) throw new IOException("Unsupported font file");
        return typeface;
    }

    @Nullable
    private static String readFamilyName(File file) {
        try {
            return LibassFontFile.getFamilyName(file);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    @Nullable
    private static Typeface createTypeface(File file) {
        try {
            return Typeface.createFromFile(file);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public record Item(String fileName, String familyName, long revision) {

        public String displayName() {
            return fileName;
        }

        private String cacheKey() {
            return fileName + ':' + revision;
        }
    }

    public record Entry(Item item, @Nullable Typeface typeface) {
    }

    private record PreparedFont(byte[] sha256, Typeface typeface, String familyName) {
    }

    private record CachedItem(@Nullable Item item, long length, long modified) {

        private boolean matches(long length, long modified) {
            return this.length == length && this.modified == modified;
        }
    }
}
