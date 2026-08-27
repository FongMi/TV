package com.fongmi.android.tv.player.mpv;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.mpvplayer.MpvPlayer;

import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.player.effect.PlayerEffect;
import com.fongmi.android.tv.player.engine.PlayerEngine;
import com.fongmi.android.tv.player.engine.PlayerEngine.SecondarySubtitleState;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.fongmi.android.tv.player.media.PlaySpec;
import com.fongmi.android.tv.setting.SubtitleSetting;

public class MpvPlayerEngine implements PlayerEngine, Player.Listener {

    private final MpvErrorMessageProvider provider;
    private final MpvPlayerEffect effect;
    private final MpvPlayer player;
    private PlaySpec spec;

    public MpvPlayerEngine(int decode, Player.Listener listener) {
        this.player = MpvUtil.buildPlayer(decode, listener);
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
    public int getAudioChannelCount() {
        return player.getAudioChannelCount();
    }

    @Override
    public PlayerEffect getEffect() {
        return effect;
    }

    @Override
    public void release() {
        player.removeListener(this);
        player.setAudioOutputListener(null);
        player.release();
    }

    @Override
    public void applySubtitleStyle() {
        MpvUtil.applySubtitleStyle(player);
    }

    @Override
    public SecondarySubtitleState getSecondarySubtitleState() {
        return new SecondarySubtitleState(player.getPrimaryTextTrackSelectionOverride(), player.getSecondaryTextTrackSelectionOverride(), player.getSecondaryTextTrackSelectionOverrides(), player.isSecondaryTextTrackSuppressed());
    }

    @Override
    public void setSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection) {
        int mode = SubtitleSetting.getSecondaryMode();
        applySecondarySubtitleMode(mode);
        if (mode != SubtitleSetting.SECONDARY_MODE_DEFAULT) player.setSecondaryTextTrackSelectionOverride(selection);
    }

    @Override
    public boolean addSubtitle(Sub sub) {
        if (sub == null || sub.isEmpty() || player.getCurrentMediaItem() == null) return false;
        if (player.getPlaybackState() == Player.STATE_IDLE || player.getPlaybackState() == Player.STATE_ENDED) return false;
        return player.addSubtitle(MediaItemFactory.buildSubConfig(sub));
    }

    @Override
    public void setDecode(int decode) {
        player.setDecode(decode);
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
        prepareAndPlay();
    }

    private void prepareAndPlay() {
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
