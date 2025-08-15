package com.github.catvod.utils;

public class Github {

    public static final String URL = "https://ghfast.top/https://raw.githubusercontent.com/FongMi/Release/fongmi";

    public static final String URL1 = "https://ghfast.top/https://raw.githubusercontent.com/xinyi1984/TVBox-TV/fongmi";

    private static String getUrl(String path, String name) {
        return URL + "/" + path + "/" + name;
    }

    private static String getUrl1(String path, String name) {
        return URL1 + "/" + path + "/" + name;
    }

    public static String getJson(boolean dev, String name) {
        return getUrl("apk/" + (dev ? "dev" : "release"), name + ".json");
    }

    public static String getApk(boolean dev, String name) {
        return getUrl1("apk/" + (dev ? "dev" : "release"), name + ".apk");
    }
}
