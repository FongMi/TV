package com.xunlei.downloadlib;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class Util {

    private static final List<String> VIDEO = Arrays.asList("avi", "flv", "mkv", "mov", "mp4", "mpeg", "mpe", "mpg", "wmv");
    private static final List<String> AUDIO = Arrays.asList("aac", "ape", "flac", "mp3", "m4a", "ogg");
    private static final String[] UNITS = new String[]{"bytes", "KB", "MB", "GB", "TB"};
    private static final long MINIMAL = 30 * 1024 * 1024;

    public static boolean isMedia(String ext, long size) {
        return (VIDEO.contains(ext) || AUDIO.contains(ext)) && size > MINIMAL;
    }

    public static String size(long size) {
        if (size <= 0) return "";
        int group = (int) (Math.log10(size) / Math.log10(1024));
        return "[" + new DecimalFormat("###0.#").format(size / Math.pow(1024, group)) + " " + UNITS[group] + "] ";
    }
}
