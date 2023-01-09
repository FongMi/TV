package com.fongmi.android.tv;

import android.text.TextUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class Github {

    public static final String A = "https://raw.githubusercontent.com/";
    public static final String B = "https://ghproxy.com/";
    public static final String C = "https://raw.iqiq.io/";
    public static final String REPO = "FongMi/TV/";
    public static final String RELEASE = "release";
    public static final String DEV = "dev";
    public static final int TIME = 5;

    private String proxy;

    private static class Loader {
        static volatile Github INSTANCE = new Github();
    }

    public static Github get() {
        return Loader.INSTANCE;
    }

    public Github() {
        check(A);
        check(B);
        check(C);
    }

    private void check(String url) {
        try {
            if (getProxy().length() > 0) return;
            int code = new OkHttpClient.Builder().connectTimeout(TIME, TimeUnit.SECONDS).build().newCall(new Request.Builder().url(url).build()).execute().code();
            if (code == 200) setProxy(url);
        } catch (IOException ignored) {
        }
    }

    private void setProxy(String url) {
        this.proxy = url.equals(B) ? url + A + REPO : url + REPO;
    }

    private String getProxy() {
        return TextUtils.isEmpty(proxy) ? "" : proxy;
    }

    public String getReleasePath(String path) {
        return getProxy() + RELEASE + path;
    }

    public String getBranchPath(String branch, String path) {
        return getProxy() + branch + path;
    }
}
