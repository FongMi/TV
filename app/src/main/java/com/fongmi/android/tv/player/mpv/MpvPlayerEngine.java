package com.fongmi.android.tv.player.mpv;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.DecoderMode;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.text.SubtitleContent;
import androidx.media3.common.text.SubtitleOffsets;
import androidx.media3.common.text.SubtitleSelectionState;
import androidx.media3.mpvplayer.MpvDecoderMode;
import androidx.media3.mpvplayer.MpvPlayer;

import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.player.effect.PlayerEffect;
import com.fongmi.android.tv.player.engine.DiscMenuController;
import com.fongmi.android.tv.player.engine.PlayerEngine;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.fongmi.android.tv.player.media.PlaySpec;
import com.fongmi.android.tv.setting.SubtitleSetting;

import org.json.JSONException;

import java.util.List;
import java.util.function.Consumer;

public class MpvPlayerEngine implements PlayerEngine, DiscMenuController, Player.Listener {

    private final MpvErrorMessageProvider provider;
    private final MpvPlayerEffect effect;
    private final MpvScriptSession scripts;
    private final MpvPlayer player;
    private PlaySpec spec;

    public MpvPlayerEngine(Player.Listener listener) {
        List<MpvScripts.Item> scriptItems = MpvUtil.readScripts();
        this.player = MpvUtil.buildPlayer(listener);
        this.scripts = new MpvScriptSession(player, scriptItems);
        this.provider = new MpvErrorMessageProvider();
        this.effect = new MpvPlayerEffect(player);
        this.player.setAudioOutputListener(effect::applyAudioEffect);
        this.player.addListener(this);
        applySecondarySubtitleMode(SubtitleSetting.getSecondaryMode());
    }

    public static boolean isAvailable() {
        return MpvUtil.isAvailable();
    }

    @Override
    public Type getType() {
        return Type.MPV;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean hasMenu() {
        return player.canOpenDiscMenu();
    }

    public void setBdjDiscMenu(boolean bdjDiscMenu) {
        player.setBdjDiscMenu(bdjDiscMenu);
    }

    public void setDiscMenuAvailabilityListener(Runnable listener) {
        player.setDiscMenuAvailabilityListener(listener);
    }

    @Override
    public boolean isActive() {
        return player.isDiscMenuActive();
    }

    @Override
    public boolean isNavigationPlayback() {
        return player.isDiscMenuInteractionInProgress();
    }

    @Override
    public boolean sendAction(String action) {
        boolean openingMenu = "menu".equals(action) || "title-menu".equals(action) || "popup".equals(action);
        return openingMenu ? player.openDiscMenu(action) : player.sendDiscNav(action);
    }

    @Override
    public void observeOpen(String action, Consumer<OpenResult> callback) {
        player.observeDiscMenuOpen(action, result -> callback.accept(switch (result) {
            case OPENED -> OpenResult.OPENED;
            case UNAVAILABLE -> OpenResult.UNAVAILABLE;
            case TIMED_OUT -> OpenResult.TIMED_OUT;
            case CANCELLED -> OpenResult.CANCELLED;
        }));
    }

    @Override
    public boolean supportsPointer() {
        return true;
    }

    @Override
    public boolean sendPointer(float x, float y, boolean activate) {
        return player.sendDiscNavPointer(x, y, activate);
    }

    @Override
    public int getAudioChannelCount() {
        return player.getAudioChannelCount();
    }

    @Override
    public PlayerEffect getEffect() {
        return effect;
    }

    @Override
    public void release() {
        scripts.release();
        player.removeListener(this);
        player.setAudioOutputListener(null);
        player.release();
    }

    @Override
    public void applySubtitleStyle() {
        MpvUtil.applySubtitleStyle(player);
    }

    public boolean runScript(MpvScripts.Item item) {
        return scripts.run(item);
    }

    public MpvScriptSession.Status getScriptStatus(String id) {
        return scripts.status(id);
    }

    public List<String> getScriptBindings() throws JSONException {
        return scripts.bindings();
    }

    public void reloadScripts(boolean reloadStartupScripts, String reloadButtonId) {
        scripts.reload(MpvUtil.readScripts(), reloadStartupScripts, reloadButtonId);
    }

    @Override
    public SubtitleSelectionState getSubtitleSelectionState() {
        return player.getSubtitleSelectionState();
    }

    @Override
    public SubtitleContent getSubtitleContent(TrackSelectionOverride selection) {
        return player.getSubtitleContentForSelection(selection);
    }

    @Override
    public SubtitleOffsets getSubtitleOffsets() {
        return player.getSubtitleOffsets();
    }

    @Override
    public void setSubtitleOffsets(SubtitleOffsets offsets) {
        player.setSubtitleOffsets(offsets);
    }

    @Override
    public boolean supportsSubtitleOffsets() {
        return true;
    }

    @Override
    public void setSubtitleContentEnabled(boolean enabled) {
        player.setSubtitleContentEnabled(enabled);
    }

    @Override
    public boolean supportsSubtitleTranscript() {
        return true;
    }

    @Override
    public boolean canRetrySubtitleContent(Format format) {
        return player.canRetrySubtitleContent(format);
    }

    @Override
    public void retrySubtitleContent(Format format) {
        player.retrySubtitleContent(format);
    }

    @Override
    public void setSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection) {
        int mode = SubtitleSetting.getSecondaryMode();
        applySecondarySubtitleMode(mode);
        if (mode != SubtitleSetting.SECONDARY_MODE_DEFAULT) player.setSecondaryTextTrackSelectionOverride(selection);
    }

    @Override
    public void restoreSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection) {
        player.setSecondaryTextTrackAutoSelectionEnabled(false);
        player.setSecondaryTextTrackSelectionOverride(selection);
    }

    @Override
    public void prepareForNewMedia() {
        applySecondarySubtitleMode(SubtitleSetting.getSecondaryMode());
    }

    @Override
    public boolean addSubtitle(Sub sub) {
        if (sub == null || sub.isEmpty() || player.getCurrentMediaItem() == null) return false;
        if (player.getPlaybackState() == Player.STATE_IDLE || player.getPlaybackState() == Player.STATE_ENDED) return false;
        return player.addSubtitle(MediaItemFactory.buildSubConfig(sub));
    }

    @Override
    public List<DecoderMode> getSupportedDecoderModes(@C.TrackType int trackType) {
        return trackType == C.TRACK_TYPE_VIDEO ? List.of(DecoderMode.AUTO, DecoderMode.HARDWARE, DecoderMode.FFMPEG) : List.of();
    }

    @Override
    @Nullable
    public DecoderMode getDecoderMode(@C.TrackType int trackType) {
        if (trackType != C.TRACK_TYPE_VIDEO) return null;
        return player.getVideoDecoderMode().toCommonDecoderMode();
    }

    @Override
    public void setDecoderMode(@C.TrackType int trackType, DecoderMode mode) {
        if (!getSupportedDecoderModes(trackType).contains(mode)) return;
        player.setVideoDecoderMode(MpvDecoderMode.fromCommonDecoderMode(mode));
    }

    @Override
    public void onTracksChanged(@NonNull Tracks tracks) {
        effect.applyVideoEffect();
    }

    @Override
    public void start(PlaySpec spec, long startPositionMs) {
        this.spec = spec;
        startInternal(startPositionMs);
    }

    private void startInternal(long startPositionMs) {
        effect.applyVideoEffect();
        effect.clearAudioEffect();
        player.setMediaItem(MediaItemFactory.from(spec), startPositionMs);
        player.prepare();
        player.play();
    }

    @Override
    public void stop() {
        player.stop();
    }

    @Override
    public String getErrorMessage(PlaybackException e) {
        return provider.get(e);
    }

    @Override
    public ErrorAction handleError(PlaybackException e) {
        return switch (e.errorCode) {
            case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED, PlaybackException.ERROR_CODE_DECODING_FAILED -> ErrorAction.DECODE;
            case PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> retryHls();
            default -> ErrorAction.FATAL;
        };
    }

    private ErrorAction retryHls() {
        if (spec == null || MimeTypes.APPLICATION_M3U8.equals(spec.getFormat())) return ErrorAction.FATAL;
        spec.setFormat(MimeTypes.APPLICATION_M3U8);
        startInternal(player.getCurrentPosition());
        return ErrorAction.RECOVERED;
    }

    private void applySecondarySubtitleMode(int mode) {
        if (mode == SubtitleSetting.SECONDARY_MODE_DEFAULT) player.resetSecondaryTextTrackSelection();
        else player.setSecondaryTextTrackAutoSelectionEnabled(mode == SubtitleSetting.SECONDARY_MODE_AUTO);
    }
}
