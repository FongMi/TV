package com.fongmi.android.tv.player.exo;

import androidx.media3.common.Format;
import androidx.media3.common.audio.AudioEffectParameters;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.exoplayer.ExoPlayer;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.player.effect.PlayerEffect;
import com.fongmi.android.tv.player.effect.audio.AudioEffectBands;
import com.fongmi.android.tv.player.effect.audio.AudioEffectConfig;
import com.fongmi.android.tv.player.effect.audio.ExoAudioEffectController;
import com.fongmi.android.tv.player.effect.video.ExoVideoEffectController;
import com.fongmi.android.tv.player.effect.video.VideoEffectProfile;
import com.fongmi.android.tv.setting.VideoSetting;

import java.util.List;

public final class ExoPlayerEffect implements PlayerEffect {

    private final ExoAudioEffectController audioEffectController;
    private final ExoVideoEffectController videoEffectController;

    private boolean previewVideoEffect;
    private boolean previewAudioEffect;
    private boolean audioEffectFailed;
    private ExoPlayer player;

    public ExoPlayerEffect() {
        this.audioEffectController = new ExoAudioEffectController();
        this.videoEffectController = new ExoVideoEffectController();
    }

    public AudioProcessor getAudioProcessor() {
        return audioEffectController.getProcessor();
    }

    public void setPlayer(ExoPlayer player) {
        this.player = player;
        player.setVideoEffects(List.of());
    }

    public void release() {
        audioEffectController.release();
    }

    @Override
    public boolean supportsVideoEffect() {
        return player.getVideoEffectsSupport() == ExoPlayer.VIDEO_EFFECTS_SUPPORTED;
    }

    @Override
    public int getVideoEffectError() {
        return switch (player.getVideoEffectsSupport()) {
            case ExoPlayer.VIDEO_EFFECTS_SUPPORTED -> 0;
            case ExoPlayer.VIDEO_EFFECTS_UNSUPPORTED_DRM -> R.string.error_video_effect_drm;
            case ExoPlayer.VIDEO_EFFECTS_UNSUPPORTED_RENDERER -> R.string.error_video_effect_decode;
            case ExoPlayer.VIDEO_EFFECTS_UNSUPPORTED_TUNNELING -> R.string.error_video_effect_tunnel;
            default -> R.string.error_video_effect_unsupported;
        };
    }

    @Override
    public void applyVideoEffect() {
        if (supportsVideoEffect()) videoEffectController.apply(player, getVideoProfile());
        else videoEffectController.clear(player);
    }

    @Override
    public void previewVideoEffect(boolean original) {
        if (previewVideoEffect == original) return;
        previewVideoEffect = original;
        applyVideoEffect();
    }

    @Override
    public boolean supportsAudioEffect() {
        return player.getAudioProcessingSupport() == ExoPlayer.AUDIO_PROCESSING_SUPPORTED && !audioEffectFailed;
    }

    @Override
    public AudioEffectBands getAudioEffectBands() {
        return AudioEffectBands.STANDARD;
    }

    @Override
    public int getAudioEffectError() {
        return switch (player.getAudioProcessingSupport()) {
            case ExoPlayer.AUDIO_PROCESSING_SUPPORTED -> audioEffectFailed ? R.string.error_audio_effect_apply : 0;
            case ExoPlayer.AUDIO_PROCESSING_UNSUPPORTED_PASSTHROUGH -> R.string.error_audio_effect_passthrough;
            default -> R.string.error_audio_effect_unsupported;
        };
    }

    @Override
    public void applyAudioEffect() {
        boolean support = player.getAudioProcessingSupport() == ExoPlayer.AUDIO_PROCESSING_SUPPORTED;
        if (support) applyAudioConfig(getAudioChannelCount());
        else clearAudioEffect();
    }

    public void clearAudioEffect() {
        audioEffectController.release();
        audioEffectFailed = false;
    }

    @Override
    public void previewAudioEffect(boolean original) {
        if (previewAudioEffect == original) return;
        previewAudioEffect = original;
        applyAudioEffect();
    }

    @Override
    public boolean supportsSkipSilence() {
        return player.isSkipSilenceSupported();
    }

    @Override
    public void setSkipSilenceEnabled(boolean enabled) {
        player.setSkipSilenceEnabled(enabled);
    }

    private VideoEffectProfile getVideoProfile() {
        return previewVideoEffect ? VideoEffectProfile.off() : VideoSetting.getAppliedProfile();
    }

    private AudioEffectParameters getAudioConfig(int channelCount) {
        return previewAudioEffect ? AudioEffectParameters.DEFAULT : AudioEffectConfig.from(getAudioEffectBands(), channelCount);
    }

    private int getAudioChannelCount() {
        Format format = player.getAudioFormat();
        return format == null ? Format.NO_VALUE : format.channelCount;
    }

    private void applyAudioConfig(int channelCount) {
        if (channelCount == Format.NO_VALUE) return;
        AudioEffectParameters config = getAudioConfig(channelCount);
        audioEffectFailed = !audioEffectController.apply(player, config);
    }
}
