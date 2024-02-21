package com.fongmi.android.tv.player;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.util.EventLogger;
import androidx.media3.ui.PlayerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.event.ErrorEvent;
import com.fongmi.android.tv.event.PlayerEvent;
import com.fongmi.android.tv.impl.ParseCallback;
import com.fongmi.android.tv.impl.SessionCallback;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Path;
import com.google.common.net.HttpHeaders;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.ui.widget.DanmakuView;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.ui.IjkVideoView;

public class Players implements Player.Listener, IMediaPlayer.Listener, AnalyticsListener, ParseCallback, DrawHandler.Callback {

    private static final String TAG = Players.class.getSimpleName();

    public static final int SYS = 0;
    public static final int IJK = 1;
    public static final int EXO = 2;

    public static final int SOFT = 0;
    public static final int HARD = 1;

    private Map<String, String> headers;
    private MediaSessionCompat session;
    private IjkVideoView ijkPlayer;
    private DanmakuView danmuView;
    private StringBuilder builder;
    private Formatter formatter;
    private ExoPlayer exoPlayer;
    private ParseJob parseJob;
    private Runnable runnable;
    private String url;
    private Sub sub;

    private long position;
    private float speed;
    private int decode;
    private int player;
    private int error;
    private int retry;
    private boolean danmuSync;

    public static boolean isExo(int type) {
        return type == EXO;
    }

    public static boolean isHard() {
        return Setting.getDecode() == HARD;
    }

    public boolean isExo() {
        return player == EXO;
    }

    public boolean isIjk() {
        return player == SYS || player == IJK;
    }

    public Players init(Activity activity) {
        player = Setting.getPlayer();
        decode = Setting.getDecode();
        builder = new StringBuilder();
        runnable = ErrorEvent::timeout;
        formatter = new Formatter(builder, Locale.getDefault());
        danmuSync = Setting.isDanmuSync();
        createSession(activity);
        return this;
    }

    private void createSession(Activity activity) {
        session = new MediaSessionCompat(activity, "TV");
        session.setMediaButtonReceiver(null);
        session.setCallback(SessionCallback.create(this));
        session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setSessionActivity(PendingIntent.getActivity(App.get(), 99, new Intent(App.get(), activity.getClass()), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        MediaControllerCompat.setMediaController(activity, session.getController());
    }

    public void set(PlayerView exo, IjkVideoView ijk) {
        releaseExo();
        releaseIjk();
        setupExo(exo);
        setupIjk(ijk);
    }

    private void setupExo(PlayerView view) {
        exoPlayer = new ExoPlayer.Builder(App.get()).setLoadControl(ExoUtil.buildLoadControl()).setRenderersFactory(ExoUtil.buildRenderersFactory()).setTrackSelector(ExoUtil.buildTrackSelector()).build();
        exoPlayer.setAudioAttributes(AudioAttributes.DEFAULT, true);
        exoPlayer.addAnalyticsListener(new EventLogger());
        exoPlayer.setHandleAudioBecomingNoisy(true);
        exoPlayer.addAnalyticsListener(this);
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.addListener(this);
        view.setPlayer(exoPlayer);
    }

    private void setupIjk(IjkVideoView view) {
        ijkPlayer = view.render(Setting.getRender()).decode(decode);
        ijkPlayer.addListener(this);
        ijkPlayer.setPlayer(player);
    }

    public void setDanmuView(DanmakuView view) {
        view.setCallback(this);
        danmuView = view;
    }

    public void setSub(Sub sub) {
        this.sub = sub;
        if (isIjk()) return;
        setMediaSource(headers, url);
    }

    public ExoPlayer exo() {
        return exoPlayer;
    }

    public IjkVideoView ijk() {
        return ijkPlayer;
    }

    public Map<String, String> getHeaders() {
        return headers == null ? new HashMap<>() : checkUa(headers);
    }

    public String getUrl() {
        return url;
    }

    public MediaSessionCompat getSession() {
        return session;
    }

    public void setMetadata(MediaMetadataCompat metadata) {
        session.setMetadata(metadata);
    }

    public int getPlayer() {
        return player;
    }

    public void setPlayer(int player) {
        if (this.player != player) stop();
        this.player = player;
    }

    public int getDecode() {
        return decode;
    }

    public void setDecode(int decode) {
        this.decode = decode;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public void reset() {
        removeTimeoutCheck();
        this.error = 0;
        this.retry = 0;
        stopParse();
    }

    public void clear() {
        this.headers = null;
        this.url = null;
    }

    public int addRetry() {
        ++retry;
        return retry;
    }

    public String stringToTime(long time) {
        return Util.format(builder, formatter, time);
    }

    public float getSpeed() {
        if (isExo() && exoPlayer != null) return exoPlayer.getPlaybackParameters().speed;
        if (isIjk() && ijkPlayer != null) return ijkPlayer.getSpeed();
        return 1.0f;
    }

    public long getPosition() {
        if (isExo() && exoPlayer != null) return exoPlayer.getCurrentPosition();
        if (isIjk() && ijkPlayer != null) return ijkPlayer.getCurrentPosition();
        return 0;
    }

    public long getDuration() {
        if (isExo() && exoPlayer != null) return exoPlayer.getDuration();
        if (isIjk() && ijkPlayer != null) return ijkPlayer.getDuration();
        return -1;
    }

    public long getBuffered() {
        if (isExo() && exoPlayer != null) return exoPlayer.getBufferedPosition();
        if (isIjk() && ijkPlayer != null) return ijkPlayer.getBufferedPosition();
        return 0;
    }

    private boolean haveDanmu() {
        return danmuView != null && danmuView.isPrepared();
    }

    public boolean haveTrack(int type) {
        if (isExo() && exoPlayer != null) return ExoUtil.haveTrack(exoPlayer.getCurrentTracks(), type);
        if (isIjk() && ijkPlayer != null) return ijkPlayer.haveTrack(type);
        return false;
    }

    public boolean isPlaying() {
        return isExo() ? exoPlayer != null && exoPlayer.isPlaying() : ijkPlayer != null && ijkPlayer.isPlaying();
    }

    public boolean isEnd() {
        if (isExo() && exoPlayer != null) return exoPlayer.getPlaybackState() == Player.STATE_ENDED;
        if (isIjk() && ijkPlayer != null) return ijkPlayer.getPlaybackState() == IjkVideoView.STATE_ENDED;
        return false;
    }

    public boolean isRelease() {
        return exoPlayer == null || ijkPlayer == null;
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(getUrl());
    }

    public boolean isVod() {
        return getDuration() > 5 * 60 * 1000;
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
        if (exoPlayer != null) exoPlayer.setPlaybackSpeed(this.speed = speed);
        if (ijkPlayer != null) ijkPlayer.setSpeed(this.speed = speed);
        return getSpeedText();
    }

    public String addSpeed() {
        float speed = getSpeed();
        float addon = speed >= 2 ? 1f : 0.25f;
        speed = speed >= 5 ? 0.25f : Math.min(speed + addon, 5.0f);
        return setSpeed(speed);
    }

    public String addSpeed(float value) {
        float speed = getSpeed();
        speed = Math.min(speed + value, 5);
        return setSpeed(speed);
    }

    public String subSpeed(float value) {
        float speed = getSpeed();
        speed = Math.max(speed - value, 0.25f);
        return setSpeed(speed);
    }

    public String toggleSpeed() {
        float speed = getSpeed();
        speed = speed == 1 ? 3f : 1f;
        return setSpeed(speed);
    }

    public void togglePlayer() {
        setPlayer(isExo() ? SYS : ++player);
    }

    public void nextPlayer() {
        setPlayer(isExo() ? IJK : EXO);
    }

    public void toggleDecode() {
        setDecode(decode == HARD ? SOFT : HARD);
        Setting.putDecode(decode);
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
        seekTo(getPosition() + time);
    }

    public void seekTo(long time) {
        if (haveDanmu()) danmuView.seekTo(time);
        if (isExo() && exoPlayer != null) exoPlayer.seekTo(time);
        if (isIjk() && ijkPlayer != null) ijkPlayer.seekTo(time);
    }

    public void play() {
        if (isPlaying() || isEnd()) return;
        session.setActive(true);
        if (isExo()) playExo();
        if (isIjk()) playIjk();
        if (haveDanmu()) danmuView.resume();
        setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
    }

    public void pause() {
        if (isExo()) pauseExo();
        if (isIjk()) pauseIjk();
        if (haveDanmu()) danmuView.pause();
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
    }

    public void stop() {
        reset();
        if (isExo()) stopExo();
        if (isIjk()) stopIjk();
        session.setActive(false);
        if (haveDanmu()) danmuView.stop();
        setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
    }

    public void release() {
        stopParse();
        session.release();
        if (isExo()) releaseExo();
        if (isIjk()) releaseIjk();
        if (haveDanmu()) danmuView.release();
    }

    public void start(Channel channel, int timeout) {
        if (channel.hasMsg()) {
            ErrorEvent.extract(channel.getMsg());
        } else if (channel.getParse() == 1) {
            startParse(channel.result(), false);
        } else if (isIllegal(channel.getUrl())) {
            ErrorEvent.url(0);
        } else {
            setMediaSource(channel, timeout);
        }
    }

    public void start(Result result, boolean useParse, int timeout) {
        if (result.hasMsg()) {
            ErrorEvent.extract(result.getMsg());
        } else if (result.getParse(1) == 1 || result.getJx() == 1) {
            startParse(result, useParse);
        } else if (isIllegal(result.getRealUrl())) {
            ErrorEvent.url(0);
        } else {
            setMediaSource(result, timeout);
        }
    }

    private int getVideoWidth() {
        return isExo() ? exoPlayer.getVideoSize().width : ijkPlayer.getVideoWidth();
    }

    private int getVideoHeight() {
        return isExo() ? exoPlayer.getVideoSize().height : ijkPlayer.getVideoHeight();
    }

    private void playExo() {
        if (exoPlayer == null) return;
        exoPlayer.play();
    }

    private void playIjk() {
        if (ijkPlayer == null) return;
        ijkPlayer.start();
    }

    private void pauseExo() {
        if (exoPlayer == null) return;
        exoPlayer.pause();
    }

    private void pauseIjk() {
        if (ijkPlayer == null) return;
        ijkPlayer.pause();
    }

    private void stopExo() {
        if (exoPlayer == null) return;
        exoPlayer.stop();
        exoPlayer.clearMediaItems();
    }

    private void stopIjk() {
        if (ijkPlayer == null) return;
        ijkPlayer.stop();
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

    private void startParse(Result result, boolean useParse) {
        stopParse();
        parseJob = ParseJob.create(this).start(result, useParse);
    }

    private void stopParse() {
        if (parseJob != null) parseJob.stop();
        parseJob = null;
    }

    public void setMediaSource(String url) {
        setMediaSource(new HashMap<>(), url);
    }

    private void setMediaSource(Result result, int timeout) {
        Logger.t(TAG).d(error + "," + result.getRealUrl());
        if (isIjk() && ijkPlayer != null) ijkPlayer.setMediaSource(IjkUtil.getSource(result), position);
        if (isExo() && exoPlayer != null) exoPlayer.setMediaSource(ExoUtil.getSource(result, sub, error), position);
        if (isExo() && exoPlayer != null) exoPlayer.prepare();
        setTimeoutCheck(result.getHeaders(), result.getRealUrl(), timeout);
    }

    private void setMediaSource(Channel channel, int timeout) {
        Logger.t(TAG).d(error + "," + channel.getUrl());
        if (isIjk() && ijkPlayer != null) ijkPlayer.setMediaSource(IjkUtil.getSource(channel));
        if (isExo() && exoPlayer != null) exoPlayer.setMediaSource(ExoUtil.getSource(channel, error));
        if (isExo() && exoPlayer != null) exoPlayer.prepare();
        setTimeoutCheck(channel.getHeaders(), channel.getUrl(), timeout);
    }

    private void setMediaSource(Map<String, String> headers, String url) {
        Logger.t(TAG).d(error + "," + url);
        if (isIjk() && ijkPlayer != null) ijkPlayer.setMediaSource(IjkUtil.getSource(headers, url), position);
        if (isExo() && exoPlayer != null) exoPlayer.setMediaSource(ExoUtil.getSource(headers, url, sub, error), position);
        if (isExo() && exoPlayer != null) exoPlayer.prepare();
        setTimeoutCheck(headers, url, Constant.TIMEOUT_PLAY);
    }

    private void setTimeoutCheck(Map<String, String> headers, String url, int timeout) {
        App.post(runnable, timeout);
        this.headers = headers;
        PlayerEvent.state(0);
        this.url = url;
    }

    private void removeTimeoutCheck() {
        App.removeCallbacks(runnable);
    }

    public void setTrack(List<Track> tracks) {
        for (Track track : tracks) setTrack(track);
    }

    private void setTrack(Track item) {
        if (item.isExo(player)) setTrackExo(item);
        if (item.isIjk(player)) setTrackIjk(item);
    }

    private void setTrackExo(Track item) {
        if (item.isSelected()) {
            ExoUtil.selectTrack(exoPlayer, item.getGroup(), item.getTrack());
        } else {
            ExoUtil.deselectTrack(exoPlayer, item.getGroup(), item.getTrack());
        }
    }

    private void setTrackIjk(Track item) {
        if (item.isSelected()) {
            ijkPlayer.selectTrack(item.getType(), item.getTrack());
        } else {
            ijkPlayer.deselectTrack(item.getType(), item.getTrack());
        }
    }

    private void setPlaybackState(int state) {
        long actions = PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        session.setPlaybackState(new PlaybackStateCompat.Builder().setActions(actions).setState(state, getPosition(), getSpeed()).build());
    }

    private boolean isIllegal(String url) {
        Uri uri = UrlUtil.uri(url);
        String host = UrlUtil.host(uri);
        String scheme = UrlUtil.scheme(uri);
        if (scheme.equals("data")) return false;
        return scheme.isEmpty() || scheme.equals("file") ? !Path.exists(url) : host.isEmpty();
    }

    public static Map<String, String> checkUa(Map<String, String> headers) {
        if (Setting.getUa().isEmpty()) return headers;
        for (Map.Entry<String, String> header : headers.entrySet()) if (header.getKey().equalsIgnoreCase(HttpHeaders.USER_AGENT)) return headers;
        headers.put(HttpHeaders.USER_AGENT, Setting.getUa());
        return headers;
    }

    public Uri getUri() {
        return getUrl().startsWith("file://") || getUrl().startsWith("/") ? FileUtil.getShareUri(getUrl()) : Uri.parse(getUrl());
    }

    public String[] getHeaderArray() {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : getHeaders().entrySet()) list.addAll(Arrays.asList(entry.getKey(), entry.getValue()));
        return list.toArray(new String[0]);
    }

    public void checkData(Intent data) {
        try {
            if (data == null || data.getExtras() == null) return;
            int position = data.getExtras().getInt("position", 0);
            String endBy = data.getExtras().getString("end_by", "");
            if (endBy.equals("playback_completion")) ActionEvent.next();
            if (endBy.equals("user")) seekTo(position);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onParseSuccess(Map<String, String> headers, String url, String from) {
        if (!TextUtils.isEmpty(from)) Notify.show(ResUtil.getString(R.string.parse_from, from));
        setMediaSource(headers, url);
    }

    @Override
    public void onParseError() {
        ErrorEvent.parse();
    }

    @Override
    public void onEvents(@NonNull Player player, @NonNull Player.Events events) {
        if (!events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED, Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_TIMELINE_CHANGED, Player.EVENT_PLAYBACK_PARAMETERS_CHANGED, Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_REPEAT_MODE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED, Player.EVENT_MEDIA_METADATA_CHANGED)) return;
        setPlaybackState(isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, int percent) {
        setPlaybackState(isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED);
    }

    @Override
    public void onPlayerError(@NonNull PlaybackException error) {
        ErrorEvent.url(ExoUtil.getRetry(this.error = error.errorCode));
        setPlaybackState(PlaybackStateCompat.STATE_ERROR);
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
    public void onInfo(IMediaPlayer mp, int what, int extra) {
        switch (what) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                PlayerEvent.state(Player.STATE_BUFFERING);
                break;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
            case IMediaPlayer.MEDIA_INFO_VIDEO_SEEK_RENDERING_START:
            case IMediaPlayer.MEDIA_INFO_AUDIO_SEEK_RENDERING_START:
                PlayerEvent.ready();
                break;
        }
    }

    @Override
    public boolean onError(IMediaPlayer mp, int what, int extra) {
        setPlaybackState(PlaybackStateCompat.STATE_ERROR);
        ErrorEvent.url(1);
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

    @Override
    public void prepared() {
        App.post(() -> {
            if (danmuView == null) return;
            if (isPlaying() && danmuView.isPrepared()) danmuView.start(getPosition());
            if (Setting.isDanmu()) danmuView.show();
            else danmuView.hide();
        });
    }

    @Override
    public void updateTimer(DanmakuTimer timer) {
        if (danmuSync) App.post(() -> timer.update(getPosition()));
        else if (speed != 1) timer.add((long) (timer.lastInterval() * (speed - 1)));
    }

    @Override
    public void danmakuShown(BaseDanmaku danmaku) {
    }

    @Override
    public void drawingFinished() {
    }
}
