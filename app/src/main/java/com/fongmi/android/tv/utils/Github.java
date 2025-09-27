package com.fongmi.android.tv.utils;

public class Github {

    public static final String URL = "https://gongdian.top/fongmi";

    // 添加更新链接常量
    public static final String UPDATE_REDIRECT_URL = "https://gongdian.top/fongmi/download";

    // 获取更新重定向链接
    public static String getUpdateRedirectUrl() {
        return UPDATE_REDIRECT_URL;
    }

    private static String getUrl(String path, String name) {
        return URL + "/" + path + "/" + name;
    }

    public static String getJson(boolean dev, String name) {
        return getUrl("apk/" + (dev ? "dev" : "release"), name + ".json");
    }

    public static String getApk(boolean dev, String name) {
        return getUrl("apk/" + (dev ? "dev" : "release"), name + ".apk");
    }
}

