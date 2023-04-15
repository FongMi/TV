package com.fongmi.android.tv.player;

import androidx.annotation.NonNull;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.ui.PlayerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.event.ErrorEvent;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.impl.ParseCallback;
import com.fongmi.android.tv.player.parse.ParseJob;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.crawler.SpiderDebug;
import com.google.common.collect.ImmutableList;

import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.ui.IjkVideoView;

public class Players implements Player.Listener, IMediaPlayer.OnInfoListener, IMediaPlayer.OnErrorListener, IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener, AnalyticsListener, ParseCallback {

    private IjkVideoView ijkPlayer;
    private StringBuilder builder;
    private Formatter formatter;
    private ExoPlayer exoPlayer;
    private ParseJob parseJob;
    private Runnable runnable;
    private int errorCode;
    private int timeout;
    private int retry;
    private int decode;
    private int player;

    public boolean isExo() {
        return player == 0;
    }

    public boolean isIjk() {
        return player == 1;
    }

    public Players init() {
        player = Prefers.getPlayer();
        decode = Prefers.getDecode();
        builder = new StringBuilder();
        runnable = ErrorEvent::timeout;
        timeout = Constant.TIMEOUT_PLAY;
        formatter = new Formatter(builder, Locale.getDefault());
        return this;
    }

    public void set(PlayerView exo, IjkVideoView ijk) {
        releaseExo();
        releaseIjk();
        setupExo(exo);
        setupIjk(ijk);
    }

    private void setupExo(PlayerView view) {
        exoPlayer = new ExoPlayer.Builder(App.get()).setLoadControl(new DefaultLoadControl()).setRenderersFactory(ExoUtil.buildRenderersFactory()).setTrackSelector(ExoUtil.buildTrackSelector()).build();
        exoPlayer.addAnalyticsListener(this);
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.addListener(this);
        view.setPlayer(exoPlayer);
    }

    private void setupIjk(IjkVideoView view) {
        ijkPlayer = view;
        ijkPlayer.setDecode(decode);
        ijkPlayer.setOnInfoListener(this);
        ijkPlayer.setOnErrorListener(this);
        ijkPlayer.setOnPreparedListener(this);
        ijkPlayer.setOnCompletionListener(this);
    }

    public ExoPlayer exo() {
        return exoPlayer;
    }

    public IjkVideoView ijk() {
        return ijkPlayer;
    }

    public int getPlayer() {
        return player;
    }

    public void setPlayer(int player) {
        this.player = player;
    }

    public int getDecode() {
        return decode;
    }

    public void setDecode(int decode) {
        this.decode = decode;
    }

    public void reset() {
        removeTimeoutCheck();
        this.errorCode = 0;
        this.retry = 0;
        stopParse();
    }

    public int addRetry() {
        ++retry;
        return retry;
    }

    public String stringToTime(long time) {
        return Util.getStringForTime(builder, formatter, time);
    }

    public float getSpeed() {
        return isExo() ? exoPlayer.getPlaybackParameters().speed : ijkPlayer.getSpeed();
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

    public boolean isPlaying() {
        return isExo() ? exoPlayer != null && exoPlayer.isPlaying() : ijkPlayer != null && ijkPlayer.isPlaying();
    }

    public boolean isPortrait() {
        return getVideoHeight() > getVideoWidth();
    }

    public String getSizeText() {
        return getVideoWidth() + " x " + getVideoHeight();
    }

    public String getSpeedText() {
        return String.format(Locale.getDefault(), "%.2f", getSpeed());
    }

    public String getPlayerText() {
        return ResUtil.getStringArray(R.array.select_player)[player];
    }

    public String getDecodeText() {
        return ResUtil.getStringArray(R.array.select_decode)[decode];
    }

    public String setSpeed(float speed) {
        exoPlayer.setPlaybackSpeed(speed);
        ijkPlayer.setSpeed(speed);
        return getSpeedText();
    }

    public String addSpeed() {
        float speed = getSpeed();
        float addon = speed >= 2 ? 1f : 0.25f;
        speed = speed == 5 ? 0.25f : speed + addon;
        exoPlayer.setPlaybackSpeed(speed);
        ijkPlayer.setSpeed(speed);
        return getSpeedText();
    }

    public String toggleSpeed() {
        float speed = getSpeed();
        speed = speed == 1 ? 3f : 1f;
        exoPlayer.setPlaybackSpeed(speed);
        ijkPlayer.setSpeed(speed);
        return getSpeedText();
    }

    public void togglePlayer() {
        stop();
        setPlayer(player == 0 ? 1 : 0);
    }

    public void toggleDecode() {
        setDecode(decode == 0 ? 1 : 0);
        Prefers.putDecode(decode);
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

    public void seekTo(int time) {
        if (isExo()) exoPlayer.seekTo(getPosition() + time);
        else if (isIjk()) ijkPlayer.seekTo(getPosition() + time);
    }

    public void seekTo(long time, boolean force) {
        if (time == 0 && !force) return;
        if (isExo()) exoPlayer.seekTo(time);
        else if (isIjk()) ijkPlayer.seekTo(time);
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

    public void release() {
        stopParse();
        if (isExo()) releaseExo();
        else if (isIjk()) releaseIjk();
    }

    public boolean isRelease() {
        return exoPlayer == null || ijkPlayer == null;
    }

    public boolean isVod() {
        return getDuration() > 5 * 60 * 1000;
    }

    public void setTrack(List<Track> tracks) {
        for (Track track : tracks) setTrack(track);
    }

    public boolean haveTrack(int type) {
        if (isExo()) {
            return ExoUtil.haveTrack(exoPlayer.getCurrentTracks(), type);
        } else {
            return ijkPlayer.haveTrack(type);
        }
    }

    public void start(Channel channel) {
        if (channel.getUrl().isEmpty()) {
            ErrorEvent.url();
        } else {
            setMediaSource(channel.getHeaders(), channel.getUrl());
        }
    }

    public void start(Result result, boolean useParse, int timeout) {
        if (result.getUrl().isEmpty()) {
            ErrorEvent.url();
        } else if (result.getParse(1) == 1 || result.getJx() == 1) {
            stopParse();
            parseJob = ParseJob.create(this).start(result, useParse);
        } else {
            this.timeout = timeout;
            setMediaSource(result);
        }
    }

    private int getVideoWidth() {
        return isExo() ? exoPlayer.getVideoSize().width : ijkPlayer.getVideoWidth();
    }

    private int getVideoHeight() {
        return isExo() ? exoPlayer.getVideoSize().height : ijkPlayer.getVideoHeight();
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
        ijkPlayer.release();
    }

    private void releaseExo() {
        if (exoPlayer == null) return;
        exoPlayer.removeListener(this);
        exoPlayer.release();
        exoPlayer = null;
    }

    private void releaseIjk() {
        if (ijkPlayer == null) return;
        ijkPlayer.release();
        ijkPlayer = null;
    }

    private void stopParse() {
        if (parseJob != null) parseJob.stop();
    }

    private void setMediaSource(Result result) {
        SpiderDebug.log(errorCode + "," + result.getUrl() + "," + result.getHeaders());
        if (isIjk()) ijkPlayer.setMediaSource(result.getPlayUrl() + result.getUrl(), result.getHeaders());
        if (isExo()) exoPlayer.setMediaSource(ExoUtil.getSource(result, errorCode));
        if (isExo()) exoPlayer.prepare();
        setTimeoutCheck();
    }

    private void setMediaSource(Map<String, String> headers, String url) {
        SpiderDebug.log(errorCode + "," + url + "," + headers);
        if (isIjk()) ijkPlayer.setMediaSource(url, headers);
        if (isExo()) exoPlayer.setMediaSource(ExoUtil.getSource(headers, url, errorCode));
        if (isExo()) exoPlayer.prepare();
        setTimeoutCheck();
    }

    private void setTimeoutCheck() {
        App.post(runnable, timeout);
        PlayerEvent.state(0);
    }

    private void removeTimeoutCheck() {
        App.removeCallbacks(runnable);
    }

    private void setTrack(Track item) {
        if (item.isExo(player)) setTrackExo(item);
        if (item.isIjk(player)) setTrackIjk(item);
    }

    private void setTrackExo(Track item) {
        if (item.isSelected()) {
            exoPlayer.setTrackSelectionParameters(exoPlayer.getTrackSelectionParameters().buildUpon().setOverrideForType(new TrackSelectionOverride(exoPlayer.getCurrentTracks().getGroups().get(item.getGroup()).getMediaTrackGroup(), item.getTrack())).build());
        } else {
            exoPlayer.setTrackSelectionParameters(exoPlayer.getTrackSelectionParameters().buildUpon().setOverrideForType(new TrackSelectionOverride(exoPlayer.getCurrentTracks().getGroups().get(item.getGroup()).getMediaTrackGroup(), ImmutableList.of())).build());
        }
    }

    private void setTrackIjk(Track item) {
        if (item.isSelected()) {
            ijkPlayer.selectTrack(item.getType(), item.getTrack());
        } else {
            ijkPlayer.deselectTrack(item.getType(), item.getTrack());
        }
    }

    @Override
    public void onParseSuccess(Map<String, String> headers, String url, String from) {
        if (from.length() > 0) Notify.show(ResUtil.getString(R.string.parse_from, from));
        setMediaSource(headers, url);
    }

    @Override
    public void onParseError() {
        ErrorEvent.parse();
    }

    @Override
    public void onPlayerError(@NonNull PlaybackException error) {
        this.errorCode = error.errorCode;
        ErrorEvent.format();
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        switch (state) {
            case Player.STATE_READY:
                PlayerEvent.ready();
                break;
            case Player.STATE_BUFFERING:
            case Player.STATE_ENDED:
            case Player.STATE_IDLE:
                PlayerEvent.state(state);
                break;
        }
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, int what, int extra) {
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                PlayerEvent.state(Player.STATE_BUFFERING);
                return true;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
            case IMediaPlayer.MEDIA_INFO_VIDEO_SEEK_RENDERING_START:
            case IMediaPlayer.MEDIA_INFO_AUDIO_SEEK_RENDERING_START:
                PlayerEvent.ready();
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean onError(IMediaPlayer mp, int what, int extra) {
        ErrorEvent.format();
        return true;
    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        PlayerEvent.ready();
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        PlayerEvent.state(Player.STATE_ENDED);
    }
}
