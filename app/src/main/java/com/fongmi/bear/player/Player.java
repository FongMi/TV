package com.fongmi.bear.player;

import com.fongmi.bear.App;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;

public class Player implements com.google.android.exoplayer2.Player.Listener {

    private final ExoPlayer exoPlayer;
    private Callback callback;

    private static class Loader {
        static volatile Player INSTANCE = new Player();
    }

    public static Player get() {
        return Loader.INSTANCE;
    }

    public Player() {
        exoPlayer = new ExoPlayer.Builder(App.get()).build();
        exoPlayer.addListener(this);
    }

    public Player callback(Callback callback) {
        this.callback = callback;
        return this;
    }

    public ExoPlayer exo() {
        return exoPlayer;
    }

    public void setMediaSource(JsonObject object) {
        HashMap<String, String> headers = new HashMap<>();
        String url = object.get("url").getAsString();
        if (object.has("header")) {
            JsonObject header = JsonParser.parseString(object.get("header").getAsString()).getAsJsonObject();
            for (String key : header.keySet()) headers.put(key, header.get(key).getAsString());
        }
        exoPlayer.setMediaSource(ExoUtil.getSource(headers, url));
        exoPlayer.prepare();
        exoPlayer.play();
    }

    public void pause() {
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
    }

    public void stop() {
        if (exoPlayer != null) {
            exoPlayer.stop();
        }
    }

    public void play() {
        if (exoPlayer != null) {
            exoPlayer.play();
        }
    }

    public void release() {
        if (exoPlayer != null) {
            exoPlayer.release();
        }
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        if (state != com.google.android.exoplayer2.Player.STATE_READY) return;
        callback.onPrepared();
    }

    public interface Callback {
        void onPrepared();
    }
}
