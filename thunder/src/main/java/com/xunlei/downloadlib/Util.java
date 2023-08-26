package com.xunlei.downloadlib;

import java.util.Arrays;
import java.util.List;

public class Util {

    private static final List<String> VIDEO = Arrays.asList("avi", "flv", "mkv", "mov", "mp4", "mpeg", "mpe", "mpg", "wmv");
    private static final List<String> AUDIO = Arrays.asList("aac", "ape", "flac", "mp3", "m4a", "ogg");

    public static boolean isMedia(String ext) {
        return VIDEO.contains(ext) || AUDIO.contains(ext);
    }

    public static boolean notAd(List<String> ads, String name) {
        for (String ad : ads) if (name.contains(ad)) return false;
        return true;
    }
}
