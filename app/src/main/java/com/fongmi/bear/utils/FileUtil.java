package com.fongmi.bear.utils;

import com.fongmi.bear.App;

import java.io.File;

public class FileUtil {

    public static File getCacheDir() {
        return App.get().getExternalCacheDir();
    }

    public static String getCachePath() {
        return getCacheDir().getAbsolutePath();
    }

    public static File getCacheFile(String fileName) {
        return new File(getCacheDir(), fileName);
    }

    public static File getJar() {
        return getCacheFile("spider.jar");
    }

    public static String getJarPath() {
        return getJar().getAbsolutePath();
    }
}
