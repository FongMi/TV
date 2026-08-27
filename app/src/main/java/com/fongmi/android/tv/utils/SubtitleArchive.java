package com.fongmi.android.tv.utils;

import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Path;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class SubtitleArchive {

    private static final String ZIP = ".zip";
    private static final String CACHE_DIR = "subtitle/";
    private static final int MAX_ENTRIES = 256;
    private static final long MAX_DOWNLOAD_BYTES = 32L * 1024 * 1024;
    private static final long MAX_EXTRACTED_BYTES = 128L * 1024 * 1024;
    private static final String[] EXTENSIONS = {".srt", ".ass", ".ssa", ".vtt", ".ttml", ".xml", ".dfxp"};

    public static boolean isZip(String name, String url) {
        return hasExtension(name, ZIP) || hasExtension(url, ZIP);
    }

    public static boolean isSupported(String name, String url) {
        return isSubtitle(name) || isSubtitle(url) || isZip(name, url);
    }

    public static File getDir(String url) {
        return Path.cache(CACHE_DIR + getKey(url));
    }

    public static Download createDownload(String name, String url) {
        return Download.create(url, getFile(name, url)).maxBytes(MAX_DOWNLOAD_BYTES);
    }

    public static List<File> unzip(File archive, File dir) {
        Path.clear(dir);
        if (!FileUtil.zipDecompress(archive, dir, MAX_ENTRIES, MAX_EXTRACTED_BYTES)) Path.clear(dir);
        return findSubtitles(dir);
    }

    private static List<File> findSubtitles(File dir) {
        try (var paths = Files.walk(dir.toPath())) {
            return paths.map(java.nio.file.Path::toFile).filter(File::isFile).filter(SubtitleArchive::isSubtitle).toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private static String getKey(String url) {
        return Crypto.md5(url);
    }

    private static File getFile(String name, String url) {
        boolean archive = isZip(name, url);
        String extension = archive ? ZIP : getSubtitleExtension(name, url);
        return Path.cache(CACHE_DIR + getKey(url) + extension);
    }

    private static String getSubtitleExtension(String name, String url) {
        for (String extension : EXTENSIONS) if (hasExtension(name, extension)) return extension;
        for (String extension : EXTENSIONS) if (hasExtension(url, extension)) return extension;
        return "";
    }

    private static boolean isSubtitle(File file) {
        return isSubtitle(file.getName());
    }

    private static boolean isSubtitle(String text) {
        for (String extension : EXTENSIONS) if (hasExtension(text, extension)) return true;
        return false;
    }

    private static boolean hasExtension(String text, String extension) {
        if (text == null || text.isEmpty()) return false;
        String lower = text.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("://")) lower = stripUrlSuffix(lower);
        return lower.endsWith(extension);
    }

    private static String stripUrlSuffix(String url) {
        if (url == null || url.isEmpty()) return "";
        int query = url.indexOf('?');
        int fragment = url.indexOf('#');
        int end = query < 0 ? url.length() : query;
        if (fragment >= 0) end = Math.min(end, fragment);
        return url.substring(0, end);
    }
}
