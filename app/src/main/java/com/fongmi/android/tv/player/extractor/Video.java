package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.fongmi.android.tv.utils.UrlUtil;

public class Video implements Source.Extractor {

    @Override
    public boolean match(Uri uri) {
        return "video".equals(UrlUtil.scheme(uri));
    }

    @Override
    public String fetch(String url) throws Exception {
        return url.substring(8);
    }
}
