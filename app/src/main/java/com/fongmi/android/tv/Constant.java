package com.fongmi.android.tv;

public class Constant {

    public static final String PROXY = "https://ghproxy.com/";
    public static final String REPO = "https://raw.githubusercontent.com/FongMi/TV/";
    public static final String RELEASE = "release";

    public static String getReleasePath(String path) {
        return PROXY + REPO + RELEASE + path;
    }

    public static String getBranchPath(String branch, String path) {
        return PROXY + REPO + branch + path;
    }
}
