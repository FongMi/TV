package com.fongmi.bear.player;

import android.os.Handler;
import android.os.Looper;

import com.fongmi.bear.App;
import com.fongmi.bear.event.PlayerEvent;
import com.fongmi.bear.ui.custom.CustomWebView;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;

public class Players implements Player.Listener {

    private final CustomWebView webView;
    private final ExoPlayer exoPlayer;
    private final Handler handler;

    private static class Loader {
        static volatile Players INSTANCE = new Players();
    }

    public static Players get() {
        return Loader.INSTANCE;
    }

    public Players() {
        webView = new CustomWebView(App.get());
        handler = new Handler(Looper.getMainLooper());
        exoPlayer = new ExoPlayer.Builder(App.get()).build();
        exoPlayer.addListener(this);
    }

    public ExoPlayer exo() {
        return exoPlayer;
    }

    public String getSpeed() {
        return String.valueOf(exoPlayer.getPlaybackParameters().speed);
    }

    public String addSpeed() {
        float speed = exoPlayer.getPlaybackParameters().speed;
        exoPlayer.setPlaybackSpeed(speed = speed >= 3 ? 0.75f : speed + 0.25f);
        return String.valueOf(speed);
    }

    public boolean isIdle() {
        return exoPlayer.getPlaybackState() == Player.STATE_IDLE;
    }

    public void setMediaSource(JsonObject object) {
        HashMap<String, String> headers = new HashMap<>();
        String parse = object.get("parse").getAsString();
        String url = object.get("url").getAsString();
        if (object.has("header")) {
            JsonObject header = JsonParser.parseString(object.get("header").getAsString()).getAsJsonObject();
            for (String key : header.keySet()) headers.put(key, header.get(key).getAsString());
        }
        if (parse.equals("1")) {
            loadWebView(url);
        } else {
            setMediaSource(headers, url);
        }
    }

    private void loadWebView(String url) {
        handler.removeCallbacks(mTimer);
        handler.postDelayed(mTimer, 5000);
        handler.post(() -> webView.start(url));
    }

    public void setMediaSource(Map<String, String> headers, String url) {
        handler.post(() -> {
            handler.removeCallbacks(mTimer);
            exoPlayer.setMediaSource(ExoUtil.getSource(headers, url));
            exoPlayer.prepare();
            exoPlayer.play();
            webView.stop();
        });
    }

    public void toggle() {
        if (exoPlayer.isPlaying()) pause();
        else play();
    }

    public void pause() {
        if (exoPlayer != null) {
            exoPlayer.pause();
        }
    }

    public void stop() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.seekTo(0);
            exoPlayer.setPlaybackSpeed(1.0f);
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

    private final Runnable mTimer = new Runnable() {
        @Override
        public void run() {
            EventBus.getDefault().post(new PlayerEvent(-1));
            exoPlayer.stop();
            webView.stop();
        }
    };

    @Override
    public void onPlaybackStateChanged(int state) {
        EventBus.getDefault().post(new PlayerEvent(state));
    }
}
