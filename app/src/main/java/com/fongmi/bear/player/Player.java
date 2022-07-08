package com.fongmi.bear.player;

import com.fongmi.bear.App;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;

public class Player implements com.google.android.exoplayer2.Player.Listener {

    private ExoPlayer player;
    private Callback callback;

    private static class Loader {
        static volatile Player INSTANCE = new Player();
    }

    public static Player get() {
        return Loader.INSTANCE;
    }

    public ExoPlayer exo(Callback callback) {
        if (player == null) player = new ExoPlayer.Builder(App.get()).build();
        player.addListener(this);
        this.callback = callback;
        return player;
    }

    public void setMediaSource(JsonObject object) {
        HashMap<String, String> headers = new HashMap<>();
        String url = object.get("url").getAsString();
        if (object.has("header")) {
            JsonObject header = JsonParser.parseString(object.get("header").getAsString()).getAsJsonObject();
            for (String key : header.keySet()) headers.put(key, header.get(key).getAsString());
        }
        player.setMediaSource(ExoUtil.getSource(headers, url));
        player.prepare();
        player.play();
    }

    public void stop() {
        if (player != null && player.isPlaying()) {
            player.stop();
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
