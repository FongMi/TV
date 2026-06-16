package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.fongmi.android.tv.utils.UrlUtil;

import java.util.List;

public class Rtsp implements Extractor {

    @Override
    public boolean match(Uri uri) {
        String scheme = UrlUtil.scheme(uri);
        return List.of("rtsp", "rtspt").contains(scheme);
    }

    @Override
    public String fetch(String url) throws Exception {
        // RTSP URLs are passed directly to the player without modification
        // ExoPlayer (with FFmpeg decoder) can handle RTSP streams natively
        return url;
    }

    @Override
    public void stop() {
        // No cleanup needed for RTSP streams
    }

    @Override
    public void exit() {
        // No cleanup needed for RTSP streams
    }
}
