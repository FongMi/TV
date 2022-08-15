package com.fongmi.android.tv.player;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OKHttp;
import com.fongmi.android.tv.ui.custom.CustomWebView;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.util.Util;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Response;

public class Players implements Player.Listener {

    private CustomWebView webView;
    private StringBuilder builder;
    private Formatter formatter;
    private ExoPlayer exoPlayer;
    private String key;

    private static class Loader {
        static volatile Players INSTANCE = new Players();
    }

    public static Players get() {
        return Loader.INSTANCE;
    }

    public void init() {
        builder = new StringBuilder();
        webView = new CustomWebView(App.get());
        exoPlayer = new ExoPlayer.Builder(App.get()).build();
        formatter = new Formatter(builder, Locale.getDefault());
        exoPlayer.addListener(this);
    }

    public ExoPlayer exo() {
        return exoPlayer;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public String getTime(long time) {
        time = getCurrentPosition() + time;
        if (time > getDuration()) time = getDuration();
        else if (time < 0) time = 0;
        return getStringForTime(time);
    }

    public String getStringForTime(long time) {
        return Util.getStringForTime(builder, formatter, time);
    }

    public long getCurrentPosition() {
        return exoPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return exoPlayer.getDuration();
    }

    public void seekTo(int time) {
        exoPlayer.seekTo(getCurrentPosition() + time);
    }

    public void seekTo(long time) {
        exoPlayer.seekTo(time);
    }

    public boolean isPlaying() {
        return exoPlayer.isPlaying();
    }

    public boolean isIdle() {
        return exoPlayer.getPlaybackState() == Player.STATE_IDLE;
    }

    public void setMediaSource(Result result) {
        if (result.getUrl().isEmpty()) {
            PlayerEvent.error(R.string.error_play_load);
        } else if (result.getParse().equals("1") || result.getJx().equals("1")) {
            startParse(result);
        } else {
            setMediaSource(getHeaders(result), result.getPlayUrl() + result.getUrl());
        }
    }

    private HashMap<String, String> getHeaders(Result result) {
        HashMap<String, String> headers = new HashMap<>();
        if (result.getHeader().isEmpty()) return headers;
        return getHeaders(JsonParser.parseString(result.getHeader()));
    }

    private HashMap<String, String> getHeaders(JsonElement element) {
        HashMap<String, String> headers = new HashMap<>();
        if (!element.isJsonObject()) return headers;
        JsonObject object = element.getAsJsonObject();
        for (String key : object.keySet()) headers.put(key, object.get(key).getAsString());
        return headers;
    }

    private Parse getParse(String playUrl, boolean useParse) {
        if (useParse) return ApiConfig.get().getParse();
        if (playUrl.startsWith("json:")) return Parse.get(1, playUrl.substring(5));
        if (playUrl.startsWith("parse:")) {
            Parse parse = ApiConfig.get().getParse(playUrl.substring(6));
            if (parse != null) return parse;
        }
        return Parse.get(0, playUrl);
    }

    private void startParse(Result result) {
        boolean useParse = (result.getPlayUrl().isEmpty() && ApiConfig.get().getFlags().contains(result.getFlag())) || result.getJx().equals("1");
        Parse parse = getParse(result.getPlayUrl(), useParse);
        if (parse.getType() == 0) {
            webView.start(parse.getUrl() + result.getUrl());
        } else if (parse.getType() == 1) {
            Headers headers = new Headers.Builder().build();
            if (parse.hasHeader()) headers = Headers.of(getHeaders(parse.getHeader()));
            OKHttp.newCall(parse.getUrl() + result.getUrl(), headers).enqueue(new Callback() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {

                }
            });
        } else if (parse.getType() == 2) {

        } else if (parse.getType() == 3) {

        }
    }

    public void setMediaSource(Map<String, String> headers, String url) {
        exoPlayer.setMediaSource(ExoUtil.getSource(headers, url));
        PlayerEvent.state(0);
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
            exoPlayer.clearMediaItems();
            exoPlayer.setPlaybackSpeed(1.0f);
        }
        if (webView != null) {
            webView.stop(false);
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
    }

    @Override
    public void onPlayerError(@NonNull PlaybackException error) {
        PlayerEvent.error(R.string.error_play_format);
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        PlayerEvent.state(state);
    }
}
