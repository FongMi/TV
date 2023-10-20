package com.fongmi.android.tv.player;

import android.net.Uri;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.UrlUtil;

import java.net.URLEncoder;
import java.util.Map;

import tv.danmaku.ijk.media.player.MediaSource;

public class IjkUtil {

    public static MediaSource getSource(Result result) {
        return getSource(result.getHeaders(), result.getRealUrl());
    }

    public static MediaSource getSource(Channel channel) {
        return getSource(channel.getHeaders(), channel.getUrl());
    }

    public static MediaSource getSource(Map<String, String> headers, String url) {
        Uri uri = UrlUtil.uri(url);
        boolean m3u8Ad = Sniffer.getRegex(uri).size() > 0;
        if (m3u8Ad) uri = Uri.parse(Server.get().getAddress().concat("/m3u8?url=").concat(URLEncoder.encode(uri.toString())));
        return new MediaSource(Players.checkUa(headers), uri);
    }
}
