package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.fongmi.android.tv.utils.UrlUtil;

public class WebView implements Source.Extractor {

    @Override
    public boolean match(Uri uri) {
        return "webview".equals(UrlUtil.scheme(uri));
    }

    @Override
    public String fetch(String url) {
        return url;
    }
}
