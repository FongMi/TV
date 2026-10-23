package com.fongmi.android.tv.player;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.DecoderMode;
import androidx.media3.common.DiscMenuController;
import androidx.media3.common.DiscMenuState;
import androidx.media3.common.Format;
import androidx.media3.common.MediaChapter;
import androidx.media3.common.MediaEdition;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Timeline;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.common.text.SubtitleContent;
import androidx.media3.common.text.SubtitleOffsets;
import androidx.media3.common.text.SubtitleSelectionState;
import androidx.media3.exoplayer.iso.IsoDiscPreparation;
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
import com.fongmi.android.tv.player.exo.ExoCommand;
import com.fongmi.android.tv.player.exo.ExoPlayerEngine;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.fongmi.android.tv.player.media.PlaySpec;
import com.fongmi.android.tv.player.mpv.MpvPlayerEngine;
import com.fongmi.android.tv.player.mpv.MpvScriptSession;
import com.fongmi.android.tv.player.mpv.MpvScripts;
import com.fongmi.android.tv.player.mpv.MpvUtil;
import com.fongmi.android.tv.player.parse.ParseJob;
import com.fongmi.android.tv.player.track.TrackUtil;
import com.fongmi.android.tv.server.LogSession;
import com.fongmi.android.tv.setting.DanmakuSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.PreloadSetting;
import com.fongmi.android.tv.setting.SpeedSetting;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonObject;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerManager implements ParseCallback {

    private final PlayerEffectManager effects;
    private final Runnable runnable;
    private final Callback callback;
    private final TimeshiftPlayback timeshift;
    private PendingPreload pendingPreload;
    private PendingPreload queuedPreload;
    private PlayerEngine engine;
    private VideoSize videoSize;
    private ParseJob parseJob;
    private PlaySpec spec;
    private Player player;
    private String playbackId;
    private long playbackAttempt;
    private float speedBeforePress = Float.NaN;

    private long pendingStartPositionMs;
    private Result browseResult;
    private boolean browsePreparing;
    private boolean browseItemReady;
    private boolean danmakuEnabled;
    private boolean refreshAttempted;
    private boolean mpvDrmExoFallback;
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
        this.timeshift = new TimeshiftPlayback(this);
    }

    public void release() {
        timeshift.clear();
        App.removeCallbacks(runnable);
        stopParse();
        clearSpeedPressState();
        if (player != null) player.removeListener(listener);
        if (engine != null) engine.release();
        engine = null;
        player = null;
    }

    public Player getPlayer() {
        return player;
    }

    public TimeshiftPlayback getTimeshift() {
        return timeshift;
    }

    @Nullable
    public String getPlaybackId() {
        return playbackId;
    }

    public long getPlaybackAttempt() {
        return playbackAttempt;
    }

    @Nullable
    public JsonObject getPlaybackLog() {
        return engine == null ? null : engine.getPlaybackLog();
    }

    private void beginPlaybackAttempt(boolean newMedia) {
        if (newMedia || playbackId == null) resetPlaybackAttempt();
        playbackAttempt++;
        logPlaybackAttempt();
    }

    private void resetPlaybackAttempt() {
        playbackId = UUID.randomUUID().toString();
        playbackAttempt = 0;
    }

    private void logPlaybackAttempt() {
        if (!LogSession.get().isRunning()) return;
        String playerId = Integer.toHexString(System.identityHashCode(player));
        Log.i("TV-Playback", "player=" + playerId + " event=playbackAttempt playbackId=" + playbackId + " attempt=" + playbackAttempt);
    }

    @Nullable
    public DiscMenuController getDiscMenuController() {
        return engine != null ? engine.getDiscMenuController() : null;
    }

    public boolean isDiscMenuAvailable() {
        return !isEmpty() && player.getDiscMenuState().availability == DiscMenuState.AVAILABILITY_AVAILABLE;
    }

    public boolean runMpvScript(MpvScripts.Item item) {
        return engine instanceof MpvPlayerEngine mpv && mpv.runScript(item);
    }

    public ExoCommand.Result runExoCommand(MpvScripts.Item item, PlayerView playerView) {
        if (!(engine instanceof ExoPlayerEngine) || !item.enabled || !item.isCommand()) return ExoCommand.Result.UNAVAILABLE;
        return ExoCommand.execute(this, playerView, item.command);
    }

    public MpvScriptSession.Status getMpvScriptStatus(String id) {
        return engine instanceof MpvPlayerEngine mpv ? mpv.getScriptStatus(id) : null;
    }

    public List<String> getMpvScriptBindings() throws JSONException {
        return engine instanceof MpvPlayerEngine mpv ? mpv.getScriptBindings() : List.of();
    }

    public void reloadMpvScripts(boolean reloadStartupScripts, String reloadButtonId) {
        if (engine instanceof MpvPlayerEngine mpv) mpv.reloadScripts(reloadStartupScripts, reloadButtonId);
    }

    private void setPlayer(Player player) {
        clearSpeedPressState();
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

    public void updateKey(String oldKey, String newKey) {
        if (spec == null || !oldKey.equals(spec.getKey())) return;
        spec.setKey(newKey);
        MediaItem current = player.getCurrentMediaItem();
        if (current != null) player.replaceMediaItem(player.getCurrentMediaItemIndex(), current.buildUpon().setMediaId(newKey).build());
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
        return PreloadSetting.isEnabled() && PreloadSetting.isNextEpisodeEnabled() && engine != null && engine.getType() == PlayerEngine.Type.EXO && !player.isCurrentMediaItemLive() && !player.getDiscMenuState().navigationPlaybackOngoing && (spec == null || !IsoDiscPreparation.isIso(MediaItemFactory.from(spec)));
    }

    public boolean isPortrait() {
        return getVideoHeight() > getVideoWidth();
    }

    public boolean isLandscape() {
        return getVideoWidth() > getVideoHeight();
    }

    public boolean isLive() {
        return player.getCurrentMediaItem() != null && (timeshift.isActive() || player.isCurrentMediaItemLive());
    }

    public boolean isVod() {
        return player.getCurrentMediaItem() != null && !isLive();
    }

    public boolean canSeek() {
        return timeshift.isActive() || !isLive() && player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM);
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
        return timeshift.isActive() ? timeshift.getPosition() : player.getCurrentPosition();
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
        clearPreload();
        if (resetAudio) engine.setDecoderMode(C.TRACK_TYPE_AUDIO, DecoderMode.AUTO);
        if (resetVideo) engine.setDecoderMode(C.TRACK_TYPE_VIDEO, DecoderMode.AUTO);
        callback.onDecodeChanged();
    }

    private void changeDecoder(Runnable change) {
        boolean recover = player.getPlayerError() != null;
        clearPreload();
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
        if (targetEngine == PlayerSetting.ENGINE_MPV && !MpvUtil.checkInstalled()) return;
        PlayerSetting.putEngine(targetEngine);
        if (isEmpty() || PlayerEngineFactory.matches(engine, spec)) return;
        restartCurrent();
    }

    public String getPositionTime(long delta) {
        return Util.timeMs(Math.clamp(getPosition() + delta, 0, Math.max(0, getDuration())));
    }

    public long getDuration() {
        if (timeshift.isActive()) return timeshift.getDuration();
        long duration = engine.getOriginalDurationMs();
        return duration == C.TIME_UNSET ? player.getDuration() : duration;
    }

    public void setEnding(long endingMs) {
        if (spec != null) spec.setEnding(endingMs);
        if (pendingPreload != null) pendingPreload.spec().setEnding(endingMs);
        if (queuedPreload != null) queuedPreload.spec().setEnding(endingMs);
        if (engine != null) engine.setEnding(endingMs);
    }

    public boolean isEndingManaged() {
        return engine != null && engine.isEndingManaged();
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

    public boolean selectChapter(MediaChapter chapter) {
        return player.selectChapter(chapter);
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

    public boolean startSpeedPress(float speed) {
        if (!player.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) return false;
        if (Float.isNaN(speedBeforePress)) speedBeforePress = getSpeed();
        setSpeed(speed);
        return true;
    }

    public void endSpeedPress() {
        if (Float.isNaN(speedBeforePress)) return;
        float speed = speedBeforePress;
        clearSpeedPressState();
        setSpeed(speed);
    }

    private void clearSpeedPressState() {
        speedBeforePress = Float.NaN;
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
        if (track.isSecondary()) setSecondaryTrack(track);
        else setPrimaryTrack(track);
    }

    private void setSecondaryTrack(Track track) {
        TrackSelectionOverride selection = track.isSelected() ? TrackUtil.findSelection(player, track) : null;
        if (track.isSelected() && selection == null) return;
        engine.restoreSecondarySubtitleSelection(selection);
        track.key(getKey()).save();
        clearPendingTrackRestore(track.getType(), track.getRole());
    }

    private void setPrimaryTrack(Track track) {
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
        timeshift.clear();
        endSpeedPress();
        pendingPreload = null;
        queuedPreload = null;
        engine.stop();
        stopParse();
    }

    public void clearMediaItems() {
        timeshift.clear();
        endSpeedPress();
        clearPreload();
        player.clearMediaItems();
    }

    public void prepareForReplacement() {
        timeshift.clear();
        endSpeedPress();
        reset();
        clearPreload();
        stopParse();
    }

    public boolean isRepeatOne() {
        return player.getRepeatMode() == Player.REPEAT_MODE_ONE;
    }

    public void setRepeatOne(boolean repeat) {
        player.setRepeatMode(repeat ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
    }

    public void replay(long positionMs) {
        if (timeshift.isActive()) timeshift.seekTo(positionMs);
        else if (positionMs == C.TIME_UNSET) player.seekToDefaultPosition();
        else player.seekTo(positionMs);
        player.play();
    }

    public void seekTo(long time) {
        if (timeshift.isActive()) timeshift.seekTo(time);
        else player.seekTo(time);
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

    public boolean preload(PlaySpec spec, long startPositionMs, Object tag) {
        if (!canPreloadNext() || spec == null || !PlayerEngineFactory.canPreload(spec) || !PlayerEngineFactory.matches(engine, spec)) return false;
        clearPreload();
        PendingPreload preload = new PendingPreload(spec.checkUa(), Math.max(0, startPositionMs), tag);
        pendingPreload = preload;
        startPreloadIfReady();
        return pendingPreload == preload || queuedPreload == preload;
    }

    public boolean seekToPreloaded(Object tag) {
        return queuedPreload != null && queuedPreload.tag() == tag && engine.seekToNext();
    }

    public void clearPreload() {
        pendingPreload = null;
        queuedPreload = null;
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
        if (!refreshAttempted) {
            refreshAttempted = true;
            if (callback.onRefresh()) return;
        }
        callback.onError(engine.getErrorMessage(e));
    }

    private void fallbackToExoForDrm() {
        mpvDrmExoFallback = true;
        PlaybackSnapshot snapshot = PlaybackSnapshot.capture(this);
        replaceEngine(PlayerEngineFactory.create(PlayerEngine.Type.EXO, listener));
        engine.prepareForNewMedia();
        beginPlaybackAttempt(false);
        engine.start(spec.checkUa(), snapshot.positionMs());
        snapshot.restorePlayerState(this);
    }

    private void retryDecode(PlaybackException e) {
        if (!engine.switchDecoderForRetry(e)) {
            callback.onError(engine.getErrorMessage(e));
            return;
        }
        callback.onDecodeChanged();
        restartCurrent();
    }

    private void restartCurrent() {
        if (spec == null || spec.getUrl() == null) return;
        endSpeedPress();
        PlayerEngine.Type previousType = engine.getType();
        boolean live = player.isCurrentMediaItemLive() && player.isCurrentMediaItemDynamic() && !timeshift.isReplaying();
        PlaybackSnapshot snapshot = PlaybackSnapshot.capture(this);
        spec.setIsoEditionIndex(snapshot.selectedIsoEditionIndex());
        ensureEngine(spec.checkUa());
        startCurrent(live && previousType != engine.getType() ? C.TIME_UNSET : snapshot.positionMs());
        snapshot.restorePlayerState(this);
    }

    private void onPlayTimeout() {
        callback.onError(ResUtil.getString(R.string.error_play_timeout));
        stop();
    }

    private void ensureEngine(PlaySpec spec) {
        if (mpvDrmExoFallback) {
            if (engine.getType() != PlayerEngine.Type.EXO || engine.needsRebuild()) replaceEngine(PlayerEngineFactory.create(PlayerEngine.Type.EXO, listener));
            return;
        }
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

    public boolean browse(Result result, String key, MediaMetadata metadata, long startPositionMs) {
        reset();
        clear();
        stopParse();
        spec = PlaySpec.from(result, key, metadata);
        boolean needsParse = result.needParse() || result.isUseParse();
        if (needsParse) player.stop();
        startMediaItem(Constant.TIMEOUT_PLAY, startPositionMs, true, false);
        if (!browseItemReady) return false;
        pendingStartPositionMs = startPositionMs;
        browseResult = needsParse || player.getPlaybackState() == Player.STATE_IDLE ? result : null;
        return true;
    }

    public void prepareBrowse() {
        Result result = browseResult;
        browseResult = null;
        if (result != null && (result.needParse() || result.isUseParse())) {
            parse(spec.getKey(), result, result.isUseParse(), spec.getMetadata(), pendingStartPositionMs);
            browsePreparing = true;
        } else if (!browsePreparing && (result == null || browseItemReady) && player.getPlaybackState() == Player.STATE_IDLE) {
            App.post(runnable, Constant.TIMEOUT_PLAY);
            player.prepare();
            callback.onPrepare();
        }
    }

    public void cancelBrowse() {
        if (browseResult == null && !browsePreparing) return;
        stop();
        clearMediaItems();
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
        parse(key, result, useParse, metadata, startPositionMs, 0);
    }

    public void parse(String key, Result result, boolean useParse, MediaMetadata metadata, long startPositionMs, long endingMs) {
        stopParse();
        pendingStartPositionMs = startPositionMs;
        spec = PlaySpec.fromParse(result, key, metadata).setEnding(endingMs);
        parseJob = ParseJob.create(this).start(result, useParse);
    }

    private void stopParse() {
        if (parseJob != null) parseJob.stop();
        parseJob = null;
        pendingStartPositionMs = C.TIME_UNSET;
        browseResult = null;
        browsePreparing = false;
    }

    private void setMediaItem(long timeout, long startPositionMs, boolean newMedia) {
        if (spec == null || spec.getUrl() == null) return;
        startMediaItem(timeout, startPositionMs, newMedia, true);
    }

    private void startMediaItem(long timeout, long startPositionMs, boolean newMedia, boolean prepare) {
        endSpeedPress();
        if (newMedia && refreshAttempted) spec.refresh();
        if (newMedia) mpvDrmExoFallback = false;
        boolean playWhenReady = player.getPlayWhenReady();
        ensureEngine(spec.checkUa());
        if (newMedia) timeshift.start(spec);
        beginPlaybackAttempt(newMedia);
        if (newMedia) engine.prepareForNewMedia();
        pendingPreload = null;
        queuedPreload = null;
        initTrack = false;
        pendingTrackRestore.clear();
        if (!prepare) {
            browseItemReady = engine.setMediaItem(spec, startPositionMs);
            player.setPlayWhenReady(playWhenReady);
        } else if (browsePreparing) {
            prepareBrowseItem(startPositionMs, playWhenReady);
        } else {
            engine.start(spec, startPositionMs);
        }
        notifyDanmakuSourceChanged();
        if (prepare) {
            App.post(runnable, timeout);
            callback.onPrepare();
        }
    }

    private void prepareBrowseItem(long startPositionMs, boolean playWhenReady) {
        if (engine.setMediaItem(spec, startPositionMs)) {
            player.setPlayWhenReady(playWhenReady);
            player.prepare();
        }
        browsePreparing = false;
    }

    private void startCurrent() {
        startCurrent(getPosition());
    }

    private void startCurrent(long startPositionMs) {
        if (timeshift.isActive()) timeshift.seekTo(startPositionMs);
        else setMediaItem(Constant.TIMEOUT_PLAY, startPositionMs, false);
    }

    void applyTimeshift(String url, String format, Map<String, String> headers) {
        boolean playWhenReady = player.getPlayWhenReady();
        spec.setUrl(url);
        spec.setFormat(format);
        spec.setHeaders(new HashMap<>(headers));
        setMediaItem(Constant.TIMEOUT_PLAY, C.TIME_UNSET, false);
        player.setPlayWhenReady(playWhenReady);
    }

    void onTimeshiftChanged() {
        callback.onTimeshiftChanged();
    }

    void onTimeshiftError() {
        Notify.show(R.string.error_play_url);
    }

    private void startPreloadIfReady() {
        PendingPreload preload = pendingPreload;
        if (preload == null || player.getPlaybackState() != Player.STATE_READY) return;
        pendingPreload = null;
        queuedPreload = preload;
        if (!engine.preload(preload.spec(), preload.startPositionMs(), () -> onPreloadError(preload))) onPreloadError(preload);
    }

    private void onPreloadError(PendingPreload preload) {
        if (pendingPreload != preload && queuedPreload != preload) return;
        clearPreload();
        callback.onPreloadError(preload.tag());
    }

    @Nullable
    public Uri getSelectedDanmakuUri() {
        Danmaku item = spec != null ? spec.getSelectedDanmaku() : null;
        Uri uri = item == null ? null : item.getUri();
        return timeshift.isReplaying() && LiveDanmaku.isLive(uri) ? null : uri;
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

        default void onTimeshiftChanged() {
        }

        default void onPreloadError(Object tag) {
        }

        default void onPreloadTransition(Object tag, long oldPositionMs, long oldDurationMs) {
        }

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

    private record PendingPreload(PlaySpec spec, long startPositionMs, Object tag) {
    }

    private void restoreTrackSelection() {
        pendingTrackRestore.clear();
        List<Track> tracks = Track.find(getKey());
        Track secondary = tracks.stream().filter(Track::isSecondary).findFirst().orElse(null);
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
        for (Track track : pending) if (!track.isSecondary() && !TrackUtil.setTrackSelection(player, track)) pendingTrackRestore.add(track);
        for (Track track : pending) if (track.isSecondary()) restoreSecondaryTrack(track);
    }

    private void restoreSecondaryTrack(Track track) {
        TrackSelectionOverride selection = TrackUtil.findSelection(player, track);
        if (selection == null) pendingTrackRestore.add(track);
        else engine.restoreSecondarySubtitleSelection(selection);
    }

    private void clearPendingTrackRestore(int type, int role) {
        pendingTrackRestore.removeIf(track -> track.getType() == type && track.getRole() == role);
    }

    private long getPreviousDuration(int mediaItemIndex) {
        Timeline timeline = player.getCurrentTimeline();
        long duration = mediaItemIndex >= 0 && mediaItemIndex < timeline.getWindowCount()
                ? timeline.getWindow(mediaItemIndex, new Timeline.Window()).getDurationMs() : C.TIME_UNSET;
        long originalDuration = engine.getOriginalDurationMs();
        if (originalDuration != C.TIME_UNSET) duration = originalDuration;
        return duration;
    }

    private void applyPreloadTransition(PendingPreload preload, PlaySpec next, long oldPositionMs, long duration) {
        queuedPreload = null;
        spec = next;
        beginPlaybackAttempt(true);
        endSpeedPress();
        initTrack = false;
        pendingTrackRestore.clear();
        reset();
        callback.onPreloadTransition(preload.tag(), oldPositionMs, duration);
        notifyDanmakuSourceChanged();
        if (!player.getCurrentTracks().isEmpty()) {
            initTrack = true;
            restoreTrackSelection();
            callback.onTracksChanged();
        }
    }

    private record PlaybackSnapshot(long positionMs, boolean playWhenReady, PlaybackParameters playbackParameters, int repeatMode, float volume, long audioOffsetMs, SubtitleOffsets subtitleOffsets, int selectedIsoEditionIndex) {

        private static PlaybackSnapshot capture(PlayerManager manager) {
            Player player = manager.player;
            float volume = player.isCommandAvailable(Player.COMMAND_GET_VOLUME) ? player.getVolume() : Float.NaN;
            long audioOffsetMs = player.isCommandAvailable(Player.COMMAND_GET_AUDIO_OFFSET) ? player.getAudioOffsetMs() : C.TIME_UNSET;
            SubtitleOffsets offsets = player.isCommandAvailable(Player.COMMAND_GET_TEXT_OFFSET) ? manager.getSubtitleOffsets() : null;
            int repeatMode = player.getRepeatMode();
            int selectedIsoEditionIndex = getSelectedIsoEditionIndex(manager, player);
            return new PlaybackSnapshot(manager.getPosition(), player.getPlayWhenReady(), player.getPlaybackParameters(), repeatMode, volume, audioOffsetMs, offsets, selectedIsoEditionIndex);
        }

        private static int getSelectedIsoEditionIndex(PlayerManager manager, Player player) {
            int selectedIsoEditionIndex = C.INDEX_UNSET;
            if (manager.spec != null && IsoDiscPreparation.isIso(MediaItemFactory.from(manager.spec))) {
                for (MediaEdition edition : player.getCurrentMediaEditions()) {
                    if (edition.selected) {
                        selectedIsoEditionIndex = edition.index;
                        break;
                    }
                }
            }
            return selectedIsoEditionIndex;
        }

        private void restorePlayerState(PlayerManager manager) {
            Player player = manager.player;
            if (player.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) player.setPlaybackParameters(playbackParameters);
            if (player.isCommandAvailable(Player.COMMAND_SET_REPEAT_MODE)) player.setRepeatMode(repeatMode);
            if (!Float.isNaN(volume) && player.isCommandAvailable(Player.COMMAND_SET_VOLUME)) player.setVolume(volume);
            if (audioOffsetMs != C.TIME_UNSET && player.isCommandAvailable(Player.COMMAND_SET_AUDIO_OFFSET)) player.setAudioOffsetMs(audioOffsetMs);
            if (subtitleOffsets != null && player.isCommandAvailable(Player.COMMAND_SET_TEXT_OFFSET)) {
                if (manager.supportsSubtitleOffsets()) manager.setSubtitleOffsets(subtitleOffsets);
                else player.setTextOffsetMs(subtitleOffsets.primaryMs);
            }
            if (player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) player.setPlayWhenReady(playWhenReady);
        }

    }

    private final Player.Listener listener = new Player.Listener() {

        @Override
        public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition, @NonNull Player.PositionInfo newPosition, int reason) {
            PendingPreload preload = queuedPreload;
            if (preload == null || newPosition.mediaItem == null) return;
            long duration = getPreviousDuration(oldPosition.mediaItemIndex);
            PlaySpec next = engine.consumePreload(newPosition.mediaItem);
            if (next != preload.spec()) return;
            applyPreloadTransition(preload, next, oldPosition.contentPositionMs, duration);
        }

        @Override
        public void onPlaybackStateChanged(int state) {
            if (state == Player.STATE_READY || state == Player.STATE_ENDED) App.removeCallbacks(runnable);
            if (state == Player.STATE_READY) {
                refreshAttempted = false;
                startPreloadIfReady();
            }
        }

        @Override
        public void onDiscMenuStateChanged(@NonNull DiscMenuState state) {
            if (state.phase == DiscMenuState.PHASE_OPENING) {
                App.removeCallbacks(runnable);
                clearPreload();
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
            } else if (!pendingTrackRestore.isEmpty()) {
                retryPendingTrackSelection();
            }
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
            PlaybackLog.recordError(PlaybackLog.capture(PlayerManager.this), e);
            if (LogSession.get().isRunning()) Log.e("TV-Playback", "player=" + Integer.toHexString(System.identityHashCode(player)) + " event=playerError playbackId=" + playbackId + " attempt=" + playbackAttempt + " code=" + e.getErrorCodeName());
            if (spec == null) return;
            clearPreload();
            if (engine.getType() == PlayerEngine.Type.MPV && e.errorCode == PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED && !mpvDrmExoFallback) {
                fallbackToExoForDrm();
                return;
            }
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
