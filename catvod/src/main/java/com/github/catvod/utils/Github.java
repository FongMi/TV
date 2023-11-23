package com.github.catvod.utils;

import com.github.catvod.net.OkHttp;

import java.io.File;

public class Github {

    public static final String URL = "https://my.t4tv.hz.cz";
    private static final String ABI = "armeabi-v7a";

    private static String getUrl(String path, String name) {
        return URL + "/" + path + "/" + name;
    }

    public static String getJson(String name) {
        return getUrl("apk/kitkat", name + ".json");
    }

    public static String getApk(String name) {
        return getUrl("apk/kitkat", name + ".apk");
    }

    public static String getSo(String name) {
        try {
            File file = Path.so(name);
            moveExist(Path.download(), file);
            String url = name.startsWith("http") ? name : getUrl("so/" + ABI, file.getName());
            if (file.length() < 300) Path.write(file, OkHttp.newCall(url).execute().body().bytes());
            return file.getAbsolutePath();
        } catch (Exception e) {
            return "";
        }
    }

    private static void moveExist(File path, File file) {
        File temp = new File(path, file.getName());
        if (temp.exists()) Path.move(temp, file);
    }
}
