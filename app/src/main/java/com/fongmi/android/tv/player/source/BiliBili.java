package com.fongmi.android.tv.player.source;

import android.net.Uri;

import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.utils.Sniffer;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonParser;

import okhttp3.Headers;

public class BiliBili {

    private static class Loader {
        static volatile BiliBili INSTANCE = new BiliBili();
    }

    public static BiliBili get() {
        return Loader.INSTANCE;
    }

    public String fetch(String url) {
        try {
            String room = Uri.parse(url).getPath().replace("/", "");
            String api = String.format("https://api.live.bilibili.com/room/v1/Room/playUrl?cid=%s&qn=20000&platform=h5", room);
            String result = OkHttp.newCall(api, Headers.of(HttpHeaders.USER_AGENT, Sniffer.CHROME)).execute().body().string();
            return JsonParser.parseString(result).getAsJsonObject().get("data").getAsJsonObject().get("durl").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return url;
        }
    }
}
