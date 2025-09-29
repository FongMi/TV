package com.fongmi.android.tv.utils;

public class Github {

    public static final String URL = "https://gongdian.top/fongmi/update";

    // 添加更新链接常量
    public static final String UPDATE_REDIRECT_URL = "https://gongdian.top/fongmi/download";

    // 获取更新重定向链接
    public static String getUpdateRedirectUrl() {
        return UPDATE_REDIRECT_URL;
    }

    private static String getUrl(String name) {
        return URL + "/" + name;
    }


    public static String getJson(boolean dev, String name) {
        return getUrl(name + ".json");
    }

    public static String getApk(boolean dev, String name) {
        return getUrl("apk/" + name + ".apk");
    }
}

