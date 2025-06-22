package com.github.catvod.net;

import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.google.common.net.HttpHeaders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Request;

public class OkCookieJar implements CookieJar {

    private CookieManager manager;

    private static class Loader {
        static volatile OkCookieJar INSTANCE = new OkCookieJar();
    }

    public static OkCookieJar get() {
        return Loader.INSTANCE;
    }

    private OkCookieJar() {
        try {
            manager = CookieManager.getInstance();
        } catch (Throwable ignored) {
        }
    }

    public static void setAcceptThirdPartyCookies(WebView view) {
        try {
            get().manager.setAcceptThirdPartyCookies(view, true);
        } catch (Throwable ignored) {
        }
    }

    public static void sync(HttpUrl url, Request request) {
        if (!"127.0.0.1".equals(url.host())) sync(url.toString(), request.header(HttpHeaders.COOKIE));
    }

    public static void sync(String url, String cookie) {
        try {
            if (TextUtils.isEmpty(cookie)) return;
            for (String split : cookie.split(";")) get().manager.setCookie(url, split);
            get().manager.flush();
        } catch (Throwable ignored) {
        }
    }

    private void add(List<Cookie> items, Cookie cookie) {
        if (cookie != null) items.add(cookie);
    }

    @NonNull
    @Override
    public synchronized List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        try {
            List<Cookie> items = new ArrayList<>();
            String cookie = manager.getCookie(url.toString());
            if (TextUtils.isEmpty(cookie)) return Collections.emptyList();
            if ("127.0.0.1".equals(url.host())) return Collections.emptyList();
            for (String split : cookie.split(";")) add(items, Cookie.parse(url, split));
            return items;
        } catch (Throwable e) {
            return Collections.emptyList();
        }
    }

    @Override
    public synchronized void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
        try {
            if ("127.0.0.1".equals(url.host())) return;
            for (Cookie cookie : cookies) manager.setCookie(url.toString(), cookie.toString());
            manager.flush();
        } catch (Throwable ignored) {
        }
    }
}