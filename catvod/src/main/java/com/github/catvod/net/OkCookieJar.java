package com.github.catvod.net;

import android.text.TextUtils;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;

import com.github.catvod.bean.WrappedCookie;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class OkCookieJar implements CookieJar {

    private final Set<WrappedCookie> cache;
    private final CookieManager manager;

    public OkCookieJar() {
        cache = new HashSet<>();
        manager = CookieManager.getInstance();
    }

    @NonNull
    @Override
    public synchronized List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        List<Cookie> items = new ArrayList<>();
        for (WrappedCookie item : cache) if (item.isExpired()) cache.remove(item);
        for (WrappedCookie item : cache) if (item.matches(url)) items.add(item.unwrap());
        items.addAll(fromManager(url));
        return items;
    }

    @Override
    public synchronized void saveFromResponse(@NonNull HttpUrl url, List<Cookie> cookies) {
        List<WrappedCookie> items = new ArrayList<>();
        for (Cookie cookie : cookies) items.add(WrappedCookie.wrap(cookie));
        for (Cookie cookie : cookies) manager.setCookie(url.toString(), cookie.toString());
        cache.removeAll(items);
        cache.addAll(items);
    }

    private List<Cookie> fromManager(HttpUrl url) {
        List<Cookie> items = new ArrayList<>();
        String cookie = manager.getCookie(url.toString());
        if (!TextUtils.isEmpty(cookie)) for (String split : cookie.split(";")) items.add(Cookie.parse(url, split));
        return items;
    }
}
