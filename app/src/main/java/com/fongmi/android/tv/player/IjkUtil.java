package com.fongmi.android.tv.player;

import android.net.Uri;

import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.Utils;

import java.util.Map;

import tv.danmaku.ijk.media.player.MediaSource;

public class IjkUtil {

    public static MediaSource getSource(Result result) {
        return getSource(result.getHeaders(), result.getRealUrl());
    }

    public static MediaSource getSource(Map<String, String> headers, String url) {
        Uri uri = Uri.parse(url.trim().replace("\\", ""));
        if (Sniffer.isAds(uri)) uri = Uri.parse(Server.get().getAddress().concat("/m3u8?url=").concat(url));
        return new MediaSource(Utils.checkHeaders(headers), uri);
    }
}
