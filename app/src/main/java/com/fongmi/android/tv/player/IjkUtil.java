package com.fongmi.android.tv.player;

import android.net.Uri;

import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Sniffer;

import java.util.Map;

import tv.danmaku.ijk.media.player.MediaSource;

public class IjkUtil {

    public static MediaSource getSource(Result result) {
        return getSource(result.getHeaders(), result.getRealUrl());
    }

    public static MediaSource getSource(Map<String, String> headers, String url) {
        Uri uri = Uri.parse(url.trim().replace("\\", ""));
        boolean hasAds = Sniffer.getAdsRegex(uri).size() > 0;
        if (hasAds) uri = Uri.parse(Server.get().getAddress(true).concat("/m3u8?url=").concat(url));
        return new MediaSource(headers, uri);
    }
}
