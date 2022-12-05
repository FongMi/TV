package com.fongmi.android.tv;

public class Constant {

    public static final String PROXY = "https://ghproxy.com/";
    public static final String REPO = "https://raw.githubusercontent.com/FongMi/TV/";
    public static final String RELEASE = "release";
    public static final String KITKAT = "kitkat";

    public static String getReleasePath(String path) {
        return PROXY + REPO + RELEASE + path;
    }

    public static String getKitkatPath(String path) {
        return PROXY + REPO + KITKAT + path;
    }
}
