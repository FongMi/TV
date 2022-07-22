package com.fongmi.bear.utils;

import android.os.Environment;

import com.fongmi.bear.App;

import java.io.File;
import java.io.FileOutputStream;

public class FileUtil {

    public static String getRootPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

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

    public static File getLocal(String path) {
        return new File(path.replace("file:/", getRootPath()));
    }

    public static void write(File file, byte[] data) throws Exception {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.flush();
        fos.close();
    }
}
