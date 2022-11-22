package com.fongmi.android.tv.player;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.util.Util;

import java.util.Formatter;
import java.util.Locale;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.ui.IjkVideoView;

public class Players implements Player.Listener, IMediaPlayer.OnInfoListener, IMediaPlayer.OnErrorListener, IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener, AnalyticsListener, ParseTask.Callback {

    private IjkVideoView ijkPlayer;
    private StringBuilder builder;
    private Formatter formatter;
    private ParseTask parseTask;
    private ExoPlayer exoPlayer;
    private int errorCode;
    private int retry;
    private float speed;

    public boolean isExo() {
        return Prefers.getPlayer() == 0;
    }

    public boolean isIjk() {
        return Prefers.getPlayer() == 1;
    }

    public Players init() {
        speed = 1;
        builder = new StringBuilder();
        formatter = new Formatter(builder, Locale.getDefault());
        setupExo();
        return this;
    }

    public void setupIjk(IjkVideoView view) {
        ijkPlayer = view;
        ijkPlayer.setOnInfoListener(this);
        ijkPlayer.setOnErrorListener(this);
        ijkPlayer.setOnPreparedListener(this);
        ijkPlayer.setOnCompletionListener(this);
    }

    public void setupExo() {
        DefaultTrackSelector selector = new DefaultTrackSelector(App.get());
        selector.setParameters(selector.getParameters().buildUpon().setPreferredTextLanguage("zh").build());
        DefaultRenderersFactory factory = new DefaultRenderersFactory(App.get()).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        exoPlayer = new ExoPlayer.Builder(App.get()).setLoadControl(new DefaultLoadControl()).setRenderersFactory(factory).setTrackSelector(selector).build();
        exoPlayer.addAnalyticsListener(this);
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.addListener(this);
    }

    public ExoPlayer exo() {
        return exoPlayer;
    }

    public void reset() {
        this.errorCode = 0;
        this.retry = 0;
    }

    public int getRetry() {
        return retry;
    }

    public int addRetry() {
        ++retry;
        return retry;
    }

    public float getSpeed() {
        return speed;
    }

    public String getSpeedText() {
        return String.format(Locale.getDefault(), "%.2f", speed);
    }

    public String addSpeed() {
        float addon = speed >= 2 ? 1f : 0.25f;
        speed = speed == 5 ? 0.25f : speed + addon;
        exoPlayer.setPlaybackSpeed(speed);
        ijkPlayer.setSpeed(speed);
        return getSpeedText();
    }

    public String toggleSpeed() {
        speed = speed == 1 ? 3f : 1f;
        exoPlayer.setPlaybackSpeed(speed);
        ijkPlayer.setSpeed(speed);
        return getSpeedText();
    }

    public String getPositionTime(long time) {
        time = getPosition() + time;
        if (time > getDuration()) time = getDuration();
        else if (time < 0) time = 0;
        return stringToTime(time);
    }

    public String getDurationTime() {
        long time = getDuration();
        if (time < 0) time = 0;
        return stringToTime(time);
    }

    public String stringToTime(long time) {
        return Util.getStringForTime(builder, formatter, time);
    }

    public long getPosition() {
        return isExo() ? exoPlayer.getCurrentPosition() : ijkPlayer.getCurrentPosition();
    }

    public long getDuration() {
        return isExo() ? exoPlayer.getDuration() : ijkPlayer.getDuration();
    }

    public long getBuffered() {
        return isExo() ? exoPlayer.getBufferedPosition() : ijkPlayer.getBufferedPosition();
    }

    public void seekTo(int time) {
        if (time == 0) return;
        if (isExo()) exoPlayer.seekTo(getPosition() + time);
        else if (isIjk()) ijkPlayer.seekTo(getPosition() + time);
    }

    public void seekTo(long time) {
        if (time == 0) return;
        if (isExo()) exoPlayer.seekTo(time);
        else if (isIjk()) ijkPlayer.seekTo(time);
    }

    public boolean isPlaying() {
        return isExo() ? exoPlayer.isPlaying() : ijkPlayer.isPlaying();
    }

    public boolean isVod() {
        return getDuration() > 5 * 60 * 1000;
    }

    public boolean canNext() {
        return getPosition() >= getDuration();
    }

    public void play() {
        if (isExo()) exoPlayer.play();
        else if (isIjk()) ijkPlayer.start();
    }

    public void pause() {
        if (isExo()) pauseExo();
        else if (isIjk()) pauseIjk();
    }

    public void stop() {
        reset();
        if (isExo()) stopExo();
        else if (isIjk()) stopIjk();
    }

    public void toggle() {
        if (isExo()) stopIjk();
        else if (isIjk()) stopExo();
    }

    public void release() {
        stopParse();
        if (isExo()) releaseExo();
        else if (isIjk()) releaseIjk();
    }

    public void start(Channel channel) {
        setMediaSource(channel.getHeaders(), channel.getUrl());
    }

    public void start(Result result, boolean useParse) {
        if (result.getUrl().isEmpty()) {
            PlayerEvent.error(R.string.error_play_load);
        } else if (result.getParse(1) == 1 || result.getJx() == 1) {
            stopParse();
            parseTask = ParseTask.create(this).run(result, useParse);
        } else {
            setMediaSource(result);
        }
    }

    private void pauseExo() {
        exoPlayer.pause();
    }

    private void pauseIjk() {
        ijkPlayer.pause();
    }

    private void stopExo() {
        exoPlayer.stop();
        exoPlayer.clearMediaItems();
    }

    private void stopIjk() {
        ijkPlayer.stopPlayback();
    }

    private void releaseExo() {
        stopExo();
        exoPlayer.removeListener(this);
        exoPlayer.release();
        exoPlayer = null;
    }

    private void releaseIjk() {
        stopIjk();
        ijkPlayer.release(true);
        ijkPlayer = null;
    }

    private void stopParse() {
        if (parseTask != null) parseTask.cancel();
    }

    private void setMediaSource(Result result) {
        if (isExo()) exoPlayer.setMediaSource(ExoUtil.getSource(result, errorCode));
        if (isIjk()) ijkPlayer.setMediaSource(result.getPlayUrl() + result.getUrl(), result.getHeaders());
        if (isExo()) exoPlayer.prepare();
        PlayerEvent.state(0);
    }

    private void setMediaSource(Map<String, String> headers, String url) {
        if (isExo()) exoPlayer.setMediaSource(ExoUtil.getSource(headers, url, errorCode));
        if (isIjk()) ijkPlayer.setMediaSource(url, headers);
        if (isExo()) exoPlayer.prepare();
        PlayerEvent.state(0);
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
        this.errorCode = error.errorCode;
        PlayerEvent.error(R.string.error_play_format, true);
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        PlayerEvent.state(state);
    }

    @Override
    public void onAudioSinkError(@NonNull EventTime eventTime, @NonNull Exception audioSinkError) {
        seekTo(200);
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, int what, int extra) {
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                PlayerEvent.state(Player.STATE_BUFFERING);
                return true;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                PlayerEvent.state(Player.STATE_READY);
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean onError(IMediaPlayer mp, int what, int extra) {
        PlayerEvent.error(R.string.error_play_format, true);
        return true;
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        PlayerEvent.state(Player.STATE_READY);
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        PlayerEvent.state(Player.STATE_ENDED);
    }
}
