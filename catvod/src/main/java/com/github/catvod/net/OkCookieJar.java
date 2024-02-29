package com.github.catvod.net;

import android.text.TextUtils;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class OkCookieJar implements CookieJar {

    private final CookieManager manager;

    public OkCookieJar() {
        manager = CookieManager.getInstance();
    }

    @NonNull
    @Override
    public synchronized List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        List<Cookie> items = new ArrayList<>();
        String cookie = manager.getCookie(url.toString());
        if (!TextUtils.isEmpty(cookie)) for (String split : cookie.split(";")) items.add(Cookie.parse(url, split));
        return items;
    }

    @Override
    public synchronized void saveFromResponse(@NonNull HttpUrl url, List<Cookie> cookies) {
        for (Cookie cookie : cookies) manager.setCookie(url.toString(), cookie.toString());
    }
}