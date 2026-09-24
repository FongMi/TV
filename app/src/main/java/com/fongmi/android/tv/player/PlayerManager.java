package com.fongmi.android.tv.player;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.DecoderMode;
import androidx.media3.common.Format;
import androidx.media3.common.MediaChapter;
import androidx.media3.common.MediaEdition;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.text.SubtitleContent;
import androidx.media3.common.text.SubtitleOffsets;
import androidx.media3.common.text.SubtitleSelectionState;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.danmaku.DanmakuConfig;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.bean.Track;
import com.fongmi.android.tv.impl.ParseCallback;
import com.fongmi.android.tv.player.effect.PlayerEffectManager;
import com.fongmi.android.tv.player.effect.audio.AudioEffectBands;
import com.fongmi.android.tv.player.engine.PlayerEngine;
import com.fongmi.android.tv.player.engine.PlayerEngineFactory;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.fongmi.android.tv.player.media.PlaySpec;
import com.fongmi.android.tv.player.mpv.MpvPlayerEngine;
import com.fongmi.android.tv.player.mpv.MpvScriptSession;
import com.fongmi.android.tv.player.mpv.MpvScripts;
import com.fongmi.android.tv.player.parse.ParseJob;
import com.fongmi.android.tv.player.track.TrackUtil;
import com.fongmi.android.tv.setting.DanmakuSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.PreloadSetting;
import com.fongmi.android.tv.setting.SpeedSetting;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.common.net.HttpHeaders;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerManager implements ParseCallback {

    private final PlayerEffectManager effects;
    private final Runnable runnable;
    private final Callback callback;
    private PendingPreload pendingPreload;
    private PlayerEngine engine;
    private VideoSize videoSize;
    private ParseJob parseJob;
    private PlaySpec spec;
    private Player player;

    private long pendingStartPositionMs;
    private boolean danmakuEnabled;
    private boolean refreshAttempted;
    private boolean initTrack;
    private final List<Track> pendingTrackRestore = new ArrayList<>();

    public PlayerManager(Callback callback) {
        this.callback = callback;
        this.runnable = this::onPlayTimeout;
        this.pendingStartPositionMs = C.TIME_UNSET;
        this.engine = PlayerEngineFactory.create(listener);
        this.effects = new PlayerEffectManager(() -> engine);
        this.danmakuEnabled = DanmakuSetting.isShow();
        this.player = engine.getPlayer();
    }

    public void release() {
        App.removeCallbacks(runnable);
        if (player != null) player.removeListener(listener);
        if (engine != null) engine.release();
        engine = null;
        player = null;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean runMpvScript(MpvScripts.Item item) {
        return engine instanceof MpvPlayerEngine mpv && mpv.runScript(item);
    }

    public MpvScriptSession.Status getMpvScriptStatus(String id) {
        return engine instanceof MpvPlayerEngine mpv ? mpv.getScriptStatus(id) : null;
    }

    public List<String> getMpvScriptBindings() throws JSONException {
        return engine instanceof MpvPlayerEngine mpv ? mpv.getScriptBindings() : List.of();
    }

    /** Applies managed script changes to the active MPV session. */
    public void reloadMpvScripts(boolean reloadStartupScripts, String reloadButtonId) {
        if (engine instanceof MpvPlayerEngine mpv) mpv.reloadScripts(reloadStartupScripts, reloadButtonId);
    }

    private void setPlayer(Player player) {
        this.player = player;
        callback.onPlayerRebuild(player);
    }

    public Tracks getCurrentTracks() {
        return player.getCurrentTracks();
    }

    public int getAudioChannelCount() {
        return engine == null ? Format.NO_VALUE : engine.getAudioChannelCount();
    }

    public List<MediaChapter> getCurrentMediaChapters() {
        return player.getCurrentMediaChapters();
    }

    public List<MediaEdition> getCurrentMediaEditions() {
        return player.getCurrentMediaEditions();
    }

    public MediaItem getCurrentMediaItem() {
        return player.getCurrentMediaItem();
    }

    public int getPlaybackState() {
        return player.getPlaybackState();
    }

    public boolean isPlaying() {
        return player.isPlaying();
    }

    public boolean isReleased() {
        return player == null;
    }

    public String getUrl() {
        return spec != null ? spec.getUrl() : null;
    }

    public String getKey() {
        return spec != null ? spec.getKey() : null;
    }

    public List<Danmaku> getDanmakus() {
        return spec != null ? spec.getDanmakus() : List.of();
    }

    private void notifyDanmakuSourceChanged() {
        callback.onDanmakuSourceChanged(getSelectedDanmakuUri());
    }

    public MediaMetadata getMetadata() {
        return spec != null ? spec.getMetadata() : null;
    }

    public String getMediaTitle() {
        MediaMetadata metadata = getMetadata();
        return metadata == null ? "" : MediaItemFactory.getDisplayTitle(metadata);
    }

    public void setMetadata(@NonNull MediaMetadata metadata) {
        if (spec == null || metadata.equals(spec.getMetadata())) return;
        spec.setMetadata(metadata);
        if (TextUtils.isEmpty(spec.getUrl())) return;
        MediaItem current = player.getCurrentMediaItem();
        if (current != null) player.replaceMediaItem(player.getCurrentMediaItemIndex(), current.buildUpon().setMediaMetadata(metadata).build());
    }

    public Map<String, String> getHeaders() {
        return spec == null || spec.getHeaders() == null ? new HashMap<>() : spec.getHeaders();
    }

    public float getSpeed() {
        return player.getPlaybackParameters().speed;
    }

    public boolean isEmpty() {
        return spec == null || TextUtils.isEmpty(spec.getUrl());
    }

    public boolean hasPlaySpec() {
        return spec != null;
    }

    public boolean canPreloadNext() {
        return PreloadSetting.isEnabled() && PreloadSetting.isNextEpisodeEnabled() && engine != null && engine.getType() == PlayerEngine.Type.EXO;
    }

    public boolean isPortrait() {
        return getVideoHeight() > getVideoWidth();
    }

    public boolean isLandscape() {
        return getVideoWidth() > getVideoHeight();
    }

    public boolean isLive() {
        return player.getCurrentMediaItem() != null && player.isCurrentMediaItemLive();
    }

    public boolean isVod() {
        return player.getCurrentMediaItem() != null && !player.isCurrentMediaItemLive();
    }

    public boolean haveTrack(int type) {
        return TrackUtil.count(getCurrentTracks(), type) > 0;
    }

    public boolean haveEdition() {
        return !getCurrentMediaEditions().isEmpty();
    }

    public boolean haveChapter() {
        return !getCurrentMediaChapters().isEmpty();
    }

    public boolean haveDanmaku() {
        return spec != null && spec.getSelectedDanmaku() != null;
    }

    public boolean canSetOpening(long position, long duration) {
        return position > 0 && duration > 0 && position <= Constant.getOpEdLimit(duration);
    }

    public boolean canSetEnding(long position, long duration) {
        return position > 0 && duration > 0 && duration - position <= Constant.getOpEdLimit(duration);
    }

    public int getVideoWidth() {
        return videoSize == null ? 0 : videoSize.width;
    }

    public int getVideoHeight() {
        return videoSize == null ? 0 : videoSize.height;
    }

    public long getPosition() {
        return player.getCurrentPosition();
    }

    public String getSizeText() {
        return (getVideoWidth() == 0 && getVideoHeight() == 0) ? "" : getVideoWidth() + " x " + getVideoHeight();
    }

    @Nullable
    public DecoderMode getDecoderMode(@C.TrackType int trackType) {
        return engine == null ? null : engine.getDecoderMode(trackType);
    }

    public List<DecoderMode> getSupportedDecoderModes(@C.TrackType int trackType) {
        return engine == null ? List.of() : engine.getSupportedDecoderModes(trackType);
    }

    public void setDecoderMode(@C.TrackType int trackType, DecoderMode mode) {
        if (getDecoderMode(trackType) == null || !getSupportedDecoderModes(trackType).contains(mode)) return;
        changeDecoder(() -> engine.setDecoderMode(trackType, mode));
    }

    public void resetDecoderModesForNewSession() {
        DecoderMode audioMode = getDecoderMode(C.TRACK_TYPE_AUDIO);
        DecoderMode videoMode = getDecoderMode(C.TRACK_TYPE_VIDEO);
        boolean resetAudio = audioMode != null && audioMode != DecoderMode.AUTO;
        boolean resetVideo = videoMode != null && videoMode != DecoderMode.AUTO;
        if (!resetAudio && !resetVideo) return;
        pendingPreload = null;
        if (resetAudio) engine.setDecoderMode(C.TRACK_TYPE_AUDIO, DecoderMode.AUTO);
        if (resetVideo) engine.setDecoderMode(C.TRACK_TYPE_VIDEO, DecoderMode.AUTO);
        callback.onDecodeChanged();
    }

    private void changeDecoder(Runnable change) {
        boolean recover = player.getPlayerError() != null;
        pendingPreload = null;
        change.run();
        if (recover && player.getPlaybackState() == Player.STATE_IDLE) {
            player.prepare();
            App.post(runnable, Constant.TIMEOUT_PLAY);
        }
        callback.onDecodeChanged();
    }

    public boolean canSetAudioSetting() {
        return effects.canSetAudioSetting();
    }

    public AudioEffectBands getAudioSettingBands() {
        return effects.getAudioSettingBands();
    }

    public int getAudioSettingError() {
        return effects.getAudioSettingError();
    }

    public boolean canSetVideoSetting() {
        return effects.canSetVideoSetting();
    }

    public int getVideoSettingError() {
        return effects.getVideoSettingError();
    }

    public boolean supportsVideoSharpness() {
        return effects.supportsVideoSharpness();
    }

    public int getEngine() {
        return isMpvEngine() ? PlayerSetting.ENGINE_MPV : PlayerSetting.ENGINE_EXO;
    }

    public void setEngine(int targetEngine) {
        PlayerSetting.putEngine(targetEngine);
        if (isEmpty() || PlayerEngineFactory.matches(engine, spec)) return;
        PlaybackSnapshot snapshot = PlaybackSnapshot.capture(player);
        startCurrent(snapshot.positionMs());
        snapshot.restore(player);
    }

    public String getPositionTime(long delta) {
        return Util.timeMs(Math.clamp(getPosition() + delta, 0, Math.max(0, getDuration())));
    }

    public long getDuration() {
        return player.getDuration();
    }

    public String getDurationTime() {
        return Util.timeMs(Math.max(0, getDuration()));
    }

    public void setSub(Sub sub) {
        if (sub == null || sub.isEmpty()) return;
        if (spec != null) spec.setSub(sub);
        if (engine.addSubtitle(sub)) play();
        else startCurrent();
    }

    public void selectChapter(MediaChapter chapter) {
        player.selectChapter(chapter);
    }

    public void selectEdition(MediaEdition edition) {
        player.selectEdition(edition);
    }

    public void setDanmakuConfig(DanmakuConfig config) {
        callback.onDanmakuConfigChanged(config);
    }

    public void setDanmakuEnabled(boolean enabled) {
        if (danmakuEnabled == enabled) return;
        danmakuEnabled = enabled;
        callback.onDanmakuEnabledChanged(danmakuEnabled);
    }

    public void applySubtitleStyle() {
        if (engine != null) engine.applySubtitleStyle();
    }

    public SubtitleSelectionState getSubtitleSelectionState() {
        return engine == null ? SubtitleSelectionState.EMPTY : engine.getSubtitleSelectionState();
    }

    public SubtitleContent getSubtitleContent(TrackSelectionOverride selection) {
        return engine == null ? SubtitleContent.UNSUPPORTED : engine.getSubtitleContent(selection);
    }

    public boolean canRetrySubtitleContent(Format format) {
        return engine != null && engine.canRetrySubtitleContent(format);
    }

    public void retrySubtitleContent(Format format) {
        if (engine != null) engine.retrySubtitleContent(format);
    }

    public void setSubtitleContentEnabled(boolean enabled) {
        if (engine != null) engine.setSubtitleContentEnabled(enabled);
    }

    public boolean supportsSubtitleTranscript() {
        return engine != null && engine.supportsSubtitleTranscript();
    }

    public void setSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection) {
        if (engine == null) return;
        engine.setSecondarySubtitleSelection(selection);
        clearPendingTrackRestore(C.TRACK_TYPE_TEXT, Track.ROLE_SECONDARY);
        if (selection == null) {
            Track.delete(getKey(), C.TRACK_TYPE_TEXT, Track.ROLE_SECONDARY);
            return;
        }
        Track track = TrackUtil.fromSelection(player.getCurrentTracks(), selection, Track.ROLE_SECONDARY);
        if (track != null) track.key(getKey()).save();
    }

    public void sendDanmaku(String text) {
        callback.onDanmakuSent(text);
    }

    public float setSpeed(float speed) {
        if (!player.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) return getSpeed();
        player.setPlaybackParameters(player.getPlaybackParameters().withSpeed(SpeedSetting.clamp(speed)));
        return getSpeed();
    }

    public float toggleSpeed() {
        return setSpeed(getSpeed() == 1 ? SpeedSetting.getLongPress() : 1);
    }

    public boolean supportsSkipSilence() {
        return effects.supportsSkipSilence();
    }

    public boolean isSkipSilence() {
        return effects.isSkipSilence();
    }

    public void setSkipSilenceEnabled(boolean enabled) {
        effects.setSkipSilenceEnabled(enabled);
    }

    public void setTrack(Track track) {
        engine.resetDecoderFallback(track.getType());
        if (track.isSecondary()) {
            TrackSelectionOverride selection = track.isSelected() ? TrackUtil.findSelection(player, track) : null;
            if (track.isSelected() && selection == null) return;
            engine.restoreSecondarySubtitleSelection(selection);
        } else {
            TrackSelectionOverride secondary = track.getType() == C.TRACK_TYPE_TEXT && !track.isSelected()
                    ? getSubtitleSelectionState().activeSecondarySelection : null;
            Track promoted = secondary == null ? null : TrackUtil.fromSelection(player.getCurrentTracks(), secondary, Track.ROLE_PRIMARY);
            Track secondaryOff = secondary == null ? null : TrackUtil.fromSelection(player.getCurrentTracks(), secondary, Track.ROLE_SECONDARY);
            if (!TrackUtil.setTrackSelection(player, track)) return;
            if (promoted != null && secondaryOff != null) {
                secondaryOff.setSelected(false);
                secondaryOff.key(getKey()).save();
                promoted.key(getKey()).save();
                clearPendingTrackRestore(C.TRACK_TYPE_TEXT, Track.ROLE_PRIMARY);
                clearPendingTrackRestore(C.TRACK_TYPE_TEXT, Track.ROLE_SECONDARY);
                return;
            }
        }
        track.key(getKey()).save();
        clearPendingTrackRestore(track.getType(), track.getRole());
    }

    public void setVideoSetting(int preset) {
        effects.setVideoSetting(preset);
    }

    public void refreshVideoSetting() {
        effects.refreshVideoSetting();
    }

    public void previewVideoSetting(boolean original) {
        effects.previewVideoSetting(original);
    }

    public void setAudioSetting(int preset) {
        effects.setAudioSetting(preset);
    }

    public void refreshAudioSetting() {
        effects.refreshAudioSetting();
    }

    public void previewAudioSetting(boolean original) {
        effects.previewAudioSetting(original);
    }

    private boolean isMpvEngine() {
        return engine != null && engine.getType() == PlayerEngine.Type.MPV;
    }

    public void play() {
        player.play();
    }

    public void pause() {
        player.pause();
    }

    public void stop() {
        engine.stop();
        stopParse();
    }

    public void clearMediaItems() {
        player.clearMediaItems();
    }

    public boolean isRepeatOne() {
        return player.getRepeatMode() == Player.REPEAT_MODE_ONE;
    }

    public void setRepeatOne(boolean repeat) {
        player.setRepeatMode(repeat ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
    }

    public void replay(long positionMs) {
        if (positionMs == C.TIME_UNSET) player.seekToDefaultPosition();
        else player.seekTo(positionMs);
        player.play();
    }

    public void seekTo(long time) {
        player.seekTo(time);
    }

    public long getTextOffsetMs() {
        return player.isCommandAvailable(Player.COMMAND_GET_TEXT_OFFSET) ? player.getTextOffsetMs() : 0;
    }

    public void setTextOffsetMs(long offsetMs) {
        if (player.isCommandAvailable(Player.COMMAND_SET_TEXT_OFFSET)) player.setTextOffsetMs(offsetMs);
    }

    public SubtitleOffsets getSubtitleOffsets() {
        return engine == null ? SubtitleOffsets.ZERO : engine.getSubtitleOffsets();
    }

    public void setSubtitleOffsets(SubtitleOffsets offsets) {
        if (engine != null && engine.supportsSubtitleOffsets()) engine.setSubtitleOffsets(offsets);
    }

    public boolean supportsSubtitleOffsets() {
        return engine != null && engine.supportsSubtitleOffsets();
    }

    public long getAudioOffsetMs() {
        return player.isCommandAvailable(Player.COMMAND_GET_AUDIO_OFFSET) ? player.getAudioOffsetMs() : 0;
    }

    public void setAudioOffsetMs(long offsetMs) {
        if (player.isCommandAvailable(Player.COMMAND_SET_AUDIO_OFFSET)) player.setAudioOffsetMs(offsetMs);
    }

    public void reset() {
        App.removeCallbacks(runnable);
        refreshAttempted = false;
    }

    public void clear() {
        spec = null;
    }

    public boolean preload(PlaySpec spec, long startPositionMs) {
        if (!canPreloadNext() || spec == null || !PlayerEngineFactory.canPreload(spec) || !PlayerEngineFactory.matches(engine, spec)) return false;
        pendingPreload = new PendingPreload(spec.checkUa(), Math.max(0, startPositionMs));
        startPreloadIfReady();
        return true;
    }

    public void clearPreload() {
        pendingPreload = null;
        if (engine != null) engine.clearPreload();
    }

    public void bindPlayerView(PlayerView playerView) {
        if (engine != null) engine.bindPlayerView(playerView);
    }

    public void resetTrack() {
        pendingTrackRestore.clear();
        TrackUtil.reset(player);
    }

    private void handleRefreshError(PlaybackException e) {
        if (refreshAttempted) {
            callback.onError(engine.getErrorMessage(e));
            return;
        }
        refreshAttempted = true;
        if (!callback.onRefresh()) callback.onError(engine.getErrorMessage(e));
    }

    private void retryDecode(PlaybackException e) {
        pendingPreload = null;
        if (!engine.switchDecoderForRetry(e)) {
            callback.onError(engine.getErrorMessage(e));
            return;
        }
        callback.onDecodeChanged();
        restartCurrent();
    }

    private void restartCurrent() {
        if (spec == null || spec.getUrl() == null) return;
        PlaybackSnapshot snapshot = PlaybackSnapshot.capture(player);
        startCurrent(snapshot.positionMs());
        snapshot.restore(player);
    }

    private void onPlayTimeout() {
        callback.onError(ResUtil.getString(R.string.error_play_timeout));
        stop();
    }

    private void ensureEngine(PlaySpec spec) {
        if (PlayerEngineFactory.matches(engine, spec)) return;
        replaceEngine(PlayerEngineFactory.create(spec, listener));
    }

    private void replaceEngine(PlayerEngine replacement) {
        PlayerEngine old = engine;
        player.removeListener(listener);
        engine = replacement;
        setPlayer(engine.getPlayer());
        old.release();
    }

    public void browse(PlaySpec spec, long startPositionMs) {
        reset();
        clear();
        stopParse();
        start(spec, Constant.TIMEOUT_PLAY, startPositionMs);
    }

    public void start(PlaySpec spec, long timeout) {
        start(spec, timeout, C.TIME_UNSET);
    }

    public void start(PlaySpec spec, long timeout, long startPositionMs) {
        this.spec = spec;
        setMediaItem(timeout, startPositionMs, true);
    }

    public void parse(String key, Result result, boolean useParse, MediaMetadata metadata) {
        parse(key, result, useParse, metadata, C.TIME_UNSET);
    }

    public void parse(String key, Result result, boolean useParse, MediaMetadata metadata, long startPositionMs) {
        stopParse();
        pendingStartPositionMs = startPositionMs;
        spec = PlaySpec.fromParse(result, key, metadata);
        parseJob = ParseJob.create(this).start(result, useParse);
    }

    private void stopParse() {
        if (parseJob != null) parseJob.stop();
        parseJob = null;
        pendingStartPositionMs = C.TIME_UNSET;
    }

    private void setMediaItem(long timeout, long startPositionMs) {
        setMediaItem(timeout, startPositionMs, false);
    }

    private void setMediaItem(long timeout, long startPositionMs, boolean newMedia) {
        if (spec == null || spec.getUrl() == null) return;
        ensureEngine(spec.checkUa());
        if (newMedia) engine.prepareForNewMedia();
        pendingPreload = null;
        initTrack = false;
        pendingTrackRestore.clear();
        engine.start(spec, startPositionMs);
        notifyDanmakuSourceChanged();
        App.post(runnable, timeout);
        callback.onPrepare();
    }

    private void startCurrent() {
        startCurrent(getPosition());
    }

    private void startCurrent(long startPositionMs) {
        setMediaItem(Constant.TIMEOUT_PLAY, startPositionMs);
    }

    private void startPreloadIfReady() {
        PendingPreload preload = pendingPreload;
        if (preload == null || player.getPlaybackState() != Player.STATE_READY) return;
        pendingPreload = null;
        engine.preload(preload.spec(), preload.startPositionMs());
    }

    @Nullable
    public Uri getSelectedDanmakuUri() {
        Danmaku item = spec != null ? spec.getSelectedDanmaku() : null;
        return item == null ? null : item.getUri();
    }

    public void setDanmaku(Danmaku item) {
        if (spec == null) return;
        spec.selectDanmaku(item);
        notifyDanmakuSourceChanged();
    }

    public void clearDanmaku() {
        if (spec == null) return;
        spec.clearDanmaku();
        notifyDanmakuSourceChanged();
    }

    public void toggleDanmaku(Danmaku item) {
        if (spec == null) return;
        spec.toggleDanmaku(item);
        notifyDanmakuSourceChanged();
    }

    public void addDanmaku(Danmaku item) {
        if (spec != null) spec.addDanmaku(item);
    }

    @Override
    public void onParseSuccess(Map<String, String> headers, String url, String from) {
        if (!TextUtils.isEmpty(from)) Notify.show(ResUtil.getString(R.string.parse_from, from));
        if (headers != null) headers.remove(HttpHeaders.RANGE);
        if (spec != null) spec.setHeaders(headers);
        if (spec != null) spec.setUrl(url);
        setMediaItem(Constant.TIMEOUT_PLAY, pendingStartPositionMs, true);
        pendingStartPositionMs = C.TIME_UNSET;
    }

    @Override
    public void onParseError() {
        pendingStartPositionMs = C.TIME_UNSET;
        callback.onError(ResUtil.getString(R.string.error_play_parse));
    }

    public interface Callback {

        void onPrepare();

        void onTracksChanged();

        void onDecodeChanged();

        void onMediaOptionsChanged();

        void onError(String msg);

        boolean onRefresh();

        void onPlayerRebuild(Player newPlayer);

        void onDanmakuSourceChanged(@Nullable Uri uri);

        void onDanmakuConfigChanged(DanmakuConfig config);

        void onDanmakuEnabledChanged(boolean enabled);

        void onDanmakuSent(String text);
    }

    private record PendingPreload(PlaySpec spec, long startPositionMs) {
    }

    private void restoreTrackSelection() {
        pendingTrackRestore.clear();
        List<Track> tracks = Track.find(getKey());
        Track secondary = null;
        for (Track track : tracks) {
            if (track.isSecondary()) {
                secondary = track;
                break;
            }
        }
        // A saved primary may currently occupy the native secondary slot.
        if (secondary != null) engine.restoreSecondarySubtitleSelection(null);
        pendingTrackRestore.addAll(TrackUtil.setTrackSelection(player, tracks));
        if (secondary != null && secondary.isSelected()) {
            TrackSelectionOverride selection = TrackUtil.findSelection(player, secondary);
            if (selection != null) engine.restoreSecondarySubtitleSelection(selection);
            else pendingTrackRestore.add(secondary);
        }
    }

    private void retryPendingTrackSelection() {
        List<Track> pending = new ArrayList<>(pendingTrackRestore);
        pendingTrackRestore.clear();
        for (Track track : pending) {
            if (!track.isSecondary() && !TrackUtil.setTrackSelection(player, track)) pendingTrackRestore.add(track);
        }
        for (Track track : pending) {
            if (!track.isSecondary()) continue;
            TrackSelectionOverride selection = TrackUtil.findSelection(player, track);
            if (selection == null) pendingTrackRestore.add(track);
            else engine.restoreSecondarySubtitleSelection(selection);
        }
    }

    private void clearPendingTrackRestore(int type, int role) {
        pendingTrackRestore.removeIf(track -> track.getType() == type && track.getRole() == role);
    }

    private record PlaybackSnapshot(long positionMs, boolean playWhenReady, PlaybackParameters playbackParameters, int repeatMode, float volume, long audioOffsetMs, long textOffsetMs) {

        private static PlaybackSnapshot capture(Player player) {
            float volume = player.isCommandAvailable(Player.COMMAND_GET_VOLUME) ? player.getVolume() : Float.NaN;
            long audioOffsetMs = player.isCommandAvailable(Player.COMMAND_GET_AUDIO_OFFSET) ? player.getAudioOffsetMs() : C.TIME_UNSET;
            long textOffsetMs = player.isCommandAvailable(Player.COMMAND_GET_TEXT_OFFSET) ? player.getTextOffsetMs() : C.TIME_UNSET;
            return new PlaybackSnapshot(player.getCurrentPosition(), player.getPlayWhenReady(), player.getPlaybackParameters(), player.getRepeatMode(), volume, audioOffsetMs, textOffsetMs);
        }

        private void restore(Player player) {
            if (player.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) player.setPlaybackParameters(playbackParameters);
            if (player.isCommandAvailable(Player.COMMAND_SET_REPEAT_MODE)) player.setRepeatMode(repeatMode);
            if (!Float.isNaN(volume) && player.isCommandAvailable(Player.COMMAND_SET_VOLUME)) player.setVolume(volume);
            if (audioOffsetMs != C.TIME_UNSET && player.isCommandAvailable(Player.COMMAND_SET_AUDIO_OFFSET)) player.setAudioOffsetMs(audioOffsetMs);
            if (textOffsetMs != C.TIME_UNSET && player.isCommandAvailable(Player.COMMAND_SET_TEXT_OFFSET)) player.setTextOffsetMs(textOffsetMs);
            if (player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) player.setPlayWhenReady(playWhenReady);
        }
    }

    private final Player.Listener listener = new Player.Listener() {

        @Override
        public void onPlaybackStateChanged(int state) {
            if (state == Player.STATE_READY || state == Player.STATE_ENDED) App.removeCallbacks(runnable);
            if (state == Player.STATE_READY) {
                refreshAttempted = false;
                startPreloadIfReady();
            }
        }

        @Override
        public void onVideoSizeChanged(@NonNull VideoSize size) {
            videoSize = size;
        }

        @Override
        public void onTracksChanged(@NonNull Tracks tracks) {
            if (tracks.isEmpty()) return;
            boolean initialize = !initTrack;
            if (initialize) {
                initTrack = true;
                restoreTrackSelection();
            } else if (!pendingTrackRestore.isEmpty()) retryPendingTrackSelection();
            if (initialize) callback.onTracksChanged();
        }

        @Override
        public void onMediaChaptersChanged(@NonNull List<MediaChapter> chapters) {
            callback.onMediaOptionsChanged();
        }

        @Override
        public void onMediaEditionsChanged(@NonNull List<MediaEdition> editions) {
            callback.onMediaOptionsChanged();
        }

        @Override
        public void onPlayerError(@NonNull PlaybackException e) {
            if (spec == null) return;
            PlayerEngine.ErrorAction action = engine.handleError(e);
            if (action != PlayerEngine.ErrorAction.RECOVERED) App.removeCallbacks(runnable);
            switch (action) {
                case DECODE -> retryDecode(e);
                case REFRESH -> handleRefreshError(e);
                case RECOVERED -> notifyDanmakuSourceChanged();
                case FATAL -> callback.onError(engine.getErrorMessage(e));
            }
        }
    };
}
