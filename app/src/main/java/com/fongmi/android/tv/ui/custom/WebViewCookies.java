package com.fongmi.android.tv.ui.custom;

import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.WebView;

import com.github.catvod.utils.UriUtil;

public final class WebViewCookies {

    public static String get(String url) {
        if (!isHttp(url)) return "";
        String value = CookieManager.getInstance().getCookie(url);
        return value == null || hasLineBreak(value) ? "" : value;
    }

    public static boolean set(String url, String cookie) {
        if (!isHttp(url) || cookie == null || cookie.isEmpty() || hasLineBreak(cookie)) return false;
        CookieManager manager = CookieManager.getInstance();
        manager.setAcceptCookie(true);
        manager.setCookie(url, cookie);
        manager.flush();
        return true;
    }

    private static boolean isHttp(String url) {
        String scheme = UriUtil.scheme(url);
        return "http".equals(scheme) || "https".equals(scheme);
    }

    private static boolean hasLineBreak(String value) {
        return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
    }

    static void set(WebView view, String url, String cookies) {
        if (TextUtils.isEmpty(cookies)) return;
        CookieManager manager = CookieManager.getInstance();
        manager.setAcceptCookie(true);
        manager.setAcceptThirdPartyCookies(view, true);
        for (String cookie : cookies.split(";")) {
            cookie = cookie.trim();
            if (!cookie.isEmpty()) manager.setCookie(url, cookie);
        }
        manager.flush();
    }

    private WebViewCookies() {
    }
}
