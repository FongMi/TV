package com.fongmi.bear.player;

import android.os.Handler;
import android.os.Looper;

import com.fongmi.bear.App;
import com.fongmi.bear.event.PlayerEvent;
import com.fongmi.bear.ui.custom.CustomWebView;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.util.Util;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.greenrobot.eventbus.EventBus;

import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Players implements Player.Listener {

    private CustomWebView webView;
    private StringBuilder builder;
    private Formatter formatter;
    private ExoPlayer exoPlayer;
    private Handler handler;
    private String videoKey;

    private static class Loader {
        static volatile Players INSTANCE = new Players();
    }

    public static Players get() {
        return Loader.INSTANCE;
    }

    public void init() {
        builder = new StringBuilder();
        webView = new CustomWebView(App.get());
        handler = new Handler(Looper.getMainLooper());
        exoPlayer = new ExoPlayer.Builder(App.get()).build();
        formatter = new Formatter(builder, Locale.getDefault());
        exoPlayer.addListener(this);
    }

    public ExoPlayer exo() {
        return exoPlayer;
    }

    public String getVideoKey() {
        return videoKey;
    }

    public void setVideoKey(String videoKey) {
        this.videoKey = videoKey;
    }

    public String getSpeed() {
        return String.format(Locale.getDefault(), "%.2f", exoPlayer.getPlaybackParameters().speed);
    }

    public String addSpeed() {
        float speed = exoPlayer.getPlaybackParameters().speed;
        float addon = speed >= 2 ? 1f : 0.25f;
        speed = speed >= 5 ? 0.5f : speed + addon;
        exoPlayer.setPlaybackSpeed(speed);
        return getSpeed();
    }

    public String getTime(int time) {
        return Util.getStringForTime(builder, formatter, exoPlayer.getCurrentPosition() + time);
    }

    public void seekTo(int time) {
        exoPlayer.seekTo(exoPlayer.getCurrentPosition() + time);
    }

    public boolean isIdle() {
        return exoPlayer.getPlaybackState() == Player.STATE_IDLE;
    }

    public boolean isPlaying() {
        return exoPlayer.isPlaying();
    }

    public void setMediaSource(JsonObject object) {
        String parse = object.get("parse").getAsString();
        String url = object.get("url").getAsString();
        if (parse.equals("1")) {
            loadWebView(url);
        } else {
            setMediaSource(getPlayHeader(object), url);
        }
    }

    private HashMap<String, String> getPlayHeader(JsonObject object) {
        HashMap<String, String> headers = new HashMap<>();
        if (!object.has("header")) return headers;
        String header = object.get("header").getAsString();
        JsonElement element = JsonParser.parseString(header);
        if (element.isJsonObject()) {
            object = element.getAsJsonObject();
            for (String key : object.keySet()) headers.put(key, object.get(key).getAsString());
        }
        return headers;
    }

    private void loadWebView(String url) {
        handler.removeCallbacks(mTimer);
        handler.postDelayed(mTimer, 10000);
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
            exoPlayer.removeListener(this);
            exoPlayer.release();
            exoPlayer = null;
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        if (handler != null) {
            handler = null;
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
