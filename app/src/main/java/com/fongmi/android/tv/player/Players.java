package com.fongmi.android.tv.player;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.ui.custom.CustomWebView;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.util.Util;

import java.util.Formatter;
import java.util.Locale;
import java.util.Map;

public class Players implements Player.Listener, ParseTask.Callback {

    private CustomWebView webView;
    private StringBuilder builder;
    private Formatter formatter;
    private ExoPlayer exoPlayer;
    private ParseTask parseTask;
    private String key;
    private int retry;

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

    public CustomWebView web() {
        return webView;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public int addRetry() {
        ++retry;
        return retry;
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

    public boolean canNext() {
        return getCurrentPosition() >= getDuration();
    }

    public void setMediaSource(Result result, boolean useParse) {
        if (result.getUrl().isEmpty()) {
            PlayerEvent.error(R.string.error_play_load);
        } else if (result.getParse(1) == 1 || result.getJx() == 1) {
            if (parseTask != null) parseTask.cancel();
            parseTask = ParseTask.create(this).run(result, useParse);
        } else {
            setMediaSource(result.getHeaders(), result.getPlayUrl() + result.getUrl());
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
        this.retry = 0;
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
    public void onParseSuccess(Map<String, String> headers, String url, String from) {
        if (from.length() > 0) Notify.show(ResUtil.getString(R.string.parse_from, from));
        setMediaSource(headers, url);
    }

    @Override
    public void onParseError() {
        PlayerEvent.error(R.string.error_play_parse);
    }

    @Override
    public void onPlayerError(@NonNull PlaybackException error) {
        PlayerEvent.error(R.string.error_play_format, true);
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        PlayerEvent.state(state);
    }
}
