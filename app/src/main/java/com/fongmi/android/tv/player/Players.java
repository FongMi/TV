package com.fongmi.android.tv.player;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.ui.custom.CustomWebView;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackException;
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
        handler = new Handler(Looper.getMainLooper());
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
        if (time > exoPlayer.getDuration()) time = exoPlayer.getDuration();
        else if (time < 0) time = 0;
        return Util.getStringForTime(builder, formatter, time);
    }

    public long getCurrentPosition() {
        return exoPlayer.getCurrentPosition();
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

    public void setMediaSource(Result result) {
        if (result.getParse().equals("1")) {
            loadWebView(result.getUrl());
        } else {
            setMediaSource(getPlayHeader(result), result.getUrl());
        }
    }

    private HashMap<String, String> getPlayHeader(Result result) {
        HashMap<String, String> headers = new HashMap<>();
        if (result.getHeader().isEmpty()) return headers;
        JsonElement element = JsonParser.parseString(result.getHeader());
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
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
            checkPosition();
            webView.stop();
        });
    }

    private void checkPosition() {
        History history = AppDatabase.get().getHistoryDao().find(getKey());
        if (history == null) return;
        seekTo(history.getDuration());
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
        if (webView != null) {
            webView.stop();
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
            EventBus.getDefault().post(new PlayerEvent(ResUtil.getString(R.string.error_play_parse)));
            exoPlayer.stop();
            webView.stop();
        }
    };

    @Override
    public void onPlayerError(@NonNull PlaybackException error) {
        EventBus.getDefault().post(new PlayerEvent(ResUtil.getString(R.string.error_play)));
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        EventBus.getDefault().post(new PlayerEvent(state));
    }
}
