package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

public class Push implements Source.Extractor {

    @Override
    public boolean match(Uri uri) {
        return Source.isPush(uri.toString());
    }

    @Override
    public String fetch(String url) {
        return url;
    }
}
