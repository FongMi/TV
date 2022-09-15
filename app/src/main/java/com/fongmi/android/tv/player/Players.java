package com.fongmi.android.tv.player;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.ui.custom.CustomWebView;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.util.Util;

import java.util.Formatter;
import java.util.Locale;
import java.util.Map;

public class Players implements Player.Listener, ParseTask.Callback {

    private DefaultRenderersFactory renderers;
    private DefaultTrackSelector track;
    private CustomWebView webView;
    private StringBuilder builder;
    private Formatter formatter;
    private ExoPlayer exoPlayer;
    private ParseTask parseTask;

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
        formatter = new Formatter(builder, Locale.getDefault());
        exoPlayer = create();
        exoPlayer.addListener(this);
        setFFmpeg(Prefers.isFFmpeg());
    }

    private ExoPlayer create() {
        return new ExoPlayer.Builder(App.get()).setRenderersFactory(renderers = new DefaultRenderersFactory(App.get())).setTrackSelector(track = new DefaultTrackSelector(App.get())).build();
    }

    public ExoPlayer exo() {
        return exoPlayer;
    }

    public CustomWebView web() {
        return webView;
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

    public void setFFmpeg(boolean on) {
        renderers.setExtensionRendererMode(on ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF);
    }

    public String getSpeed() {
        return String.format(Locale.getDefault(), "%.2f", exo().getPlaybackParameters().speed);
    }

    public void addSpeed() {
        float speed = exo().getPlaybackParameters().speed;
        float addon = speed >= 2 ? 1f : 0.25f;
        speed = speed == 5 ? 0.25f : speed + addon;
        exo().setPlaybackSpeed(speed);
    }

    public void resetSpeed() {
        exo().setPlaybackSpeed(1f);
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
        return exo() == null ? 0 : exo().getCurrentPosition();
    }

    public long getDuration() {
        return exo() == null ? 0 : exo().getDuration();
    }

    public void seekTo(int time) {
        if (exo() != null) exo().seekTo(getCurrentPosition() + time);
    }

    public void seekTo(long time) {
        if (exo() != null) exo().seekTo(time);
    }

    public boolean isPlaying() {
        return exo() != null && exo().isPlaying();
    }

    public boolean isIdle() {
        return exo() != null && exo().getPlaybackState() == Player.STATE_IDLE;
    }

    public boolean canNext() {
        return getCurrentPosition() >= getDuration();
    }

    public void start(Result result, boolean useParse) {
        if (result.getUrl().isEmpty()) {
            PlayerEvent.error(R.string.error_play_load);
        } else if (result.getParse(1) == 1 || result.getJx() == 1) {
            if (parseTask != null) parseTask.cancel();
            parseTask = ParseTask.create(this).run(result, useParse);
        } else {
            setMediaSource(result);
        }
    }

    private void setMediaSource(Result result) {
        exo().setMediaSource(ExoUtil.getSource(result));
        PlayerEvent.state(0);
        exo().prepare();
    }

    private void setMediaSource(Map<String, String> headers, String url) {
        exo().setMediaSource(ExoUtil.getSource(headers, url));
        PlayerEvent.state(0);
        exo().prepare();
    }

    public void play() {
        if (exo() != null) exo().play();
    }

    public void pause() {
        if (exo() != null) exo().pause();
    }

    public void stop() {
        this.retry = 0;
        exo().stop();
        exo().clearMediaItems();
        exo().setPlaybackSpeed(1.0f);
        if (webView != null) webView.stop(false);
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
