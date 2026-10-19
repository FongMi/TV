package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.github.catvod.utils.UriUtil;

public class Video implements Source.Extractor {

    @Override
    public boolean match(Uri uri) {
        return "video".equals(UriUtil.scheme(uri));
    }

    @Override
    public String fetch(String url) throws Exception {
        return url.substring(8);
    }
}
