package com.fongmi.bear.player;

import com.fongmi.bear.App;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;

public class Player {

    private ExoPlayer mPlayer;

    private static class Loader {
        static volatile Player INSTANCE = new Player();
    }

    public static Player get() {
        return Loader.INSTANCE;
    }

    public static ExoPlayer exo() {
        return get().mPlayer = get().mPlayer == null ? new ExoPlayer.Builder(App.get()).build() : get().mPlayer;
    }

    public void setMediaSource(JsonObject object) {
        HashMap<String, String> headers = new HashMap<>();
        String url = object.get("url").getAsString();
        if (object.has("header")) {
            JsonObject header = JsonParser.parseString(object.get("header").getAsString()).getAsJsonObject();
            for (String key : header.keySet()) headers.put(key, header.get(key).getAsString());
        }
        mPlayer.setMediaSource(ExoUtil.getSource(headers, url));
        mPlayer.prepare();
        mPlayer.play();
    }
}
