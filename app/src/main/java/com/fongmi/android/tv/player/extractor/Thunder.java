package com.fongmi.android.tv.player.extractor;

import com.fongmi.android.tv.player.Source;

public class Thunder implements Source.Extractor {

    public Thunder() {
    }

    @Override
    public boolean match(String scheme, String host) {
        return scheme.equalsIgnoreCase("ed2k") || scheme.equalsIgnoreCase("ftp") || scheme.equalsIgnoreCase("thunder") || scheme.equalsIgnoreCase("magnet");
    }

    @Override
    public String fetch(String url) throws Exception {
        return url;
    }

    @Override
    public void stop() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void release() {

    }
}
