package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.utils.Sniffer;
import com.github.catvod.net.OkHttp;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonParser;

import okhttp3.Headers;

public class BiliBili implements Source.Extractor {

    @Override
    public boolean match(String scheme, String host) {
        return "live.bilibili.com".equals(host);
    }

    @Override
    public String fetch(String url) throws Exception {
        String room = Uri.parse(url).getPath().replace("/", "");
        String api = String.format("https://api.live.bilibili.com/room/v1/Room/playUrl?cid=%s&qn=20000&platform=h5", room);
        String result = OkHttp.newCall(api, Headers.of(HttpHeaders.USER_AGENT, Sniffer.CHROME)).execute().body().string();
        return JsonParser.parseString(result).getAsJsonObject().get("data").getAsJsonObject().get("durl").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
    }

    @Override
    public void stop() {
    }

    @Override
    public void exit() {
    }
}
