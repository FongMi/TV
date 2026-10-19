package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.github.catvod.utils.UriUtil;

public class WebView implements Source.Extractor {

    @Override
    public boolean match(Uri uri) {
        return "webview".equals(UriUtil.scheme(uri));
    }

    @Override
    public String fetch(String url) {
        return url;
    }
}
