package com.fongmi.android.tv.api.loader;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.ui.activity.WebActivity;
import com.fongmi.android.tv.ui.custom.WebViewCookies;
import com.fongmi.quickjs.host.Host;

final class JsHost implements Host {

    static final JsHost INSTANCE = new JsHost();

    private JsHost() {
    }

    @Override
    public long getCallTimeout() {
        return Constant.TIMEOUT_VOD;
    }

    @Override
    public boolean openWeb(String options) {
        return WebActivity.openWeb(options);
    }

    @Override
    public String getCookie(String url) {
        try {
            return WebViewCookies.get(url);
        } catch (RuntimeException e) {
            return "";
        }
    }

    @Override
    public boolean setCookie(String url, String cookie) {
        try {
            return WebViewCookies.set(url, cookie);
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public String getUserAgent() {
        try {
            String value = WebActivity.getUserAgent();
            return value == null || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0 ? "" : value;
        } catch (RuntimeException e) {
            return "";
        }
    }
}
