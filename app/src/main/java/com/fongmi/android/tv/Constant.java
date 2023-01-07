package com.fongmi.android.tv;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class Constant {

    public static final String A = "https://raw.githubusercontent.com/";
    public static final String B = "https://raw.githubusercontents.com/";
    public static final String C = "https://ghproxy.com/";
    public static final String D = "https://raw.iqiq.io/";
    public static final String E = "https://raw.fastgit.org/";
    public static final String REPO = "FongMi/TV/";
    public static final String RELEASE = "release";
    private static final int TIME = 2;

    private static String getProxy() {
        if (isOk(A)) return A + REPO;
        if (isOk(B)) return B + REPO;
        if (isOk(C)) return C + A + REPO;
        if (isOk(D)) return D + REPO;
        if (isOk(E)) return E + REPO;
        return "";
    }

    private static boolean isOk(String url) {
        try {
            return new OkHttpClient.Builder().connectTimeout(TIME, TimeUnit.SECONDS).build().newCall(new Request.Builder().url(url).build()).execute().code() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    public static String getReleasePath(String path) {
        return getProxy() + RELEASE + path;
    }

    public static String getBranchPath(String branch, String path) {
        return getProxy() + branch + path;
    }
}
