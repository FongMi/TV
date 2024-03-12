package com.github.catvod.net;

import android.text.TextUtils;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class OkCookieJar implements CookieJar {

    @NonNull
    @Override
    public synchronized List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        try {
            List<Cookie> items = new ArrayList<>();
            String cookie = CookieManager.getInstance().getCookie(url.toString());
            if (!TextUtils.isEmpty(cookie)) for (String split : cookie.split(";")) items.add(Cookie.parse(url, split));
            return items;
        } catch (Throwable e) {
            return new ArrayList<>();
        }
    }

    @Override
    public synchronized void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies) {
        try {
            for (Cookie cookie : cookies) CookieManager.getInstance().setCookie(url.toString(), cookie.toString());
        } catch (Throwable ignored) {
        }
    }
}