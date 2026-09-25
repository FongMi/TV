package com.fongmi.android.tv.player.media;

import android.net.Uri;

import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.player.track.TrackUtil;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LocalSubtitleScanner {

    private static final Set<String> EXTENSIONS = Set.of("ass", "srt", "ssa", "vtt");

    private LocalSubtitleScanner() {
    }

    public static List<Sub> scan(String url) {
        return findFiles(url).stream().map(LocalSubtitleScanner::toSub).toList();
    }

    static List<File> findFiles(String url) {
        File video = getLocalFile(url);
        return video == null ? List.of() : findFiles(video);
    }

    static List<File> findFiles(File video) {
        File parent = video.isFile() ? video.getParentFile() : null;
        File[] files = parent == null ? null : parent.listFiles();
        if (files == null) return List.of();
        String videoName = removeExtension(video.getName());
        List<File> subtitles = Arrays.stream(files).filter(File::isFile).filter(LocalSubtitleScanner::isSubtitle).toList();
        List<File> matching = subtitles.stream().filter(file -> matches(videoName, file.getName())).toList();
        List<File> selected = matching.isEmpty() ? subtitles : matching;
        return selected.stream().sorted(Comparator.comparingInt((File file) -> removeExtension(file.getName()).equalsIgnoreCase(videoName) ? 0 : 1).thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private static File getLocalFile(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null) return new File(url);
            return "file".equalsIgnoreCase(uri.getScheme()) ? new File(uri) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isSubtitle(File file) {
        return EXTENSIONS.contains(getExtension(file.getName()));
    }

    private static boolean matches(String videoName, String subtitleName) {
        String video = videoName.toLowerCase(Locale.ROOT);
        String subtitle = removeExtension(subtitleName).toLowerCase(Locale.ROOT);
        return subtitle.equals(video) || subtitle.startsWith(video + ".");
    }

    private static String getExtension(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private static String removeExtension(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? name : name.substring(0, index);
    }

    private static Sub toSub(File file) {
        String name = removeExtension(file.getName());
        return Sub.from(name, Uri.fromFile(file).toString(), "", TrackUtil.getSubtitleMimeType(file.getName()));
    }
}
