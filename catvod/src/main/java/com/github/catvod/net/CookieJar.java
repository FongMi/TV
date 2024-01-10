package com.github.catvod.net;

import android.text.TextUtils;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class CookieJar implements okhttp3.CookieJar {

    private static class Loader {
        static volatile CookieJar INSTANCE = new CookieJar();
    }

    public static CookieJar get() {
        return Loader.INSTANCE;
    }

    private final CookieManager manager;

    public CookieJar() {
        manager = CookieManager.getInstance();
    }

    @Override
    public void saveFromResponse(@NonNull HttpUrl url, List<Cookie> cookies) {
        for (Cookie cookie : cookies) manager.setCookie(url.toString(), cookie.toString());
    }

    @NonNull
    @Override
    public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        return get(url);
    }

    public List<Cookie> get(HttpUrl url) {
        String cookie = manager.getCookie(url.toString());
        if (cookie == null || cookie.isEmpty()) return Collections.emptyList();
        List<Cookie> items = new ArrayList<>();
        for (String text : cookie.split(";")) items.add(Cookie.parse(url, text.trim()));
        return items;
    }

    public void remove(HttpUrl url, List<String> cookieNames, int maxAge) {
        String cookie = manager.getCookie(url.toString());
        if (TextUtils.isEmpty(cookie)) return;
        List<String> filter = cookieNames != null ? cookieNames : Arrays.asList(cookie.split(";"));
        for (String text : filter) manager.setCookie(url.toString(), text + "=;Max-Age=" + maxAge);
    }

    public void removeAll() {
        manager.removeAllCookies(null);
    }
}