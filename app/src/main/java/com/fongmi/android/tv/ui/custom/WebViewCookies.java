package com.fongmi.android.tv.ui.custom;

import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.WebView;

final class WebViewCookies {

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
