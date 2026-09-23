package com.fongmi.android.tv.api.loader;

import android.webkit.CookieManager;

import com.fongmi.android.tv.ui.activity.WebActivity;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.quickjs.host.Host;

final class JsHost implements Host {

    static final JsHost INSTANCE = new JsHost();

    private JsHost() {
    }

    @Override
    public boolean openWeb(String url, String cookieName) {
        try {
            return WebActivity.open(url, emptyToNull(cookieName));
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public String getCookie(String url) {
        if (!isHttp(url)) return "";
        try {
            return safeHeaderValue(CookieManager.getInstance().getCookie(url));
        } catch (RuntimeException e) {
            return "";
        }
    }

    @Override
    public boolean setCookie(String url, String cookie) {
        if (!isHttp(url) || cookie == null || cookie.isEmpty() || hasLineBreak(cookie)) return false;
        try {
            CookieManager manager = CookieManager.getInstance();
            manager.setAcceptCookie(true);
            manager.setCookie(url, cookie);
            manager.flush();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public String getUserAgent() {
        try {
            return safeHeaderValue(WebActivity.getUserAgent());
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static boolean isHttp(String url) {
        String scheme = UrlUtil.scheme(url);
        return "http".equals(scheme) || "https".equals(scheme);
    }

    private static boolean hasLineBreak(String value) {
        return value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0;
    }

    private static String safeHeaderValue(String value) {
        return value == null || hasLineBreak(value) ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
