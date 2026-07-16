package com.fongmi.android.tv.player.mpv;

import androidx.media3.common.Format;
import androidx.media3.common.audio.AudioEffectParameters;
import androidx.media3.mpvplayer.MpvPlayer;
import androidx.media3.mpvplayer.audio.MpvAudioFilter;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.player.effect.PlayerEffect;
import com.fongmi.android.tv.player.effect.audio.AudioEffectBands;
import com.fongmi.android.tv.player.effect.audio.AudioEffectConfig;
import com.fongmi.android.tv.player.effect.audio.MpvAudioEffectFilter;
import com.fongmi.android.tv.player.effect.video.MpvVideoEffectController;
import com.fongmi.android.tv.player.effect.video.VideoEffectProfile;
import com.fongmi.android.tv.setting.VideoSetting;

public final class MpvPlayerEffect implements PlayerEffect {

    private final MpvVideoEffectController videoEffectController;
    private final MpvPlayer player;

    private boolean previewVideoEffect;
    private boolean previewAudioEffect;
    private boolean audioEffectFailed;

    public MpvPlayerEffect(MpvPlayer player) {
        this.videoEffectController = new MpvVideoEffectController();
        this.player = player;
    }

    @Override
    public boolean supportsVideoEffect() {
        return player.getVideoEffectsSupport() == MpvPlayer.VIDEO_EFFECTS_SUPPORTED;
    }

    @Override
    public int getVideoEffectError() {
        return switch (player.getVideoEffectsSupport()) {
            case MpvPlayer.VIDEO_EFFECTS_SUPPORTED -> 0;
            case MpvPlayer.VIDEO_EFFECTS_UNSUPPORTED_DIRECT_DOLBY_VISION_OUTPUT -> R.string.error_video_effect_dolby_vision_passthrough;
            default -> R.string.error_video_effect_unsupported;
        };
    }

    @Override
    public boolean supportsVideoSharpness() {
        return player.isVideoSharpnessSupported();
    }

    @Override
    public void applyVideoEffect() {
        if (supportsVideoEffect() && (previewVideoEffect || VideoSetting.isEnabled())) videoEffectController.apply(player, getVideoProfile());
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
        return player.getAudioEffectsSupport() == MpvPlayer.AUDIO_EFFECTS_SUPPORTED && !audioEffectFailed;
    }

    @Override
    public AudioEffectBands getAudioEffectBands() {
        return AudioEffectBands.STANDARD;
    }

    @Override
    public int getAudioEffectError() {
        return switch (player.getAudioEffectsSupport()) {
            case MpvPlayer.AUDIO_EFFECTS_SUPPORTED -> audioEffectFailed ? R.string.error_audio_effect_apply : 0;
            case MpvPlayer.AUDIO_EFFECTS_UNSUPPORTED_PASSTHROUGH -> R.string.error_audio_effect_passthrough;
            default -> R.string.error_audio_effect_unsupported;
        };
    }

    @Override
    public void applyAudioEffect() {
        boolean support = player.getAudioEffectsSupport() == MpvPlayer.AUDIO_EFFECTS_SUPPORTED;
        if (support) applyAudioConfig(player.getAudioChannelCount());
        else clearAudioEffect();
    }

    public void clearAudioEffect() {
        audioEffectFailed = !player.setAudioFilter(MpvAudioFilter.EMPTY);
    }

    @Override
    public void previewAudioEffect(boolean original) {
        if (previewAudioEffect == original) return;
        previewAudioEffect = original;
        applyAudioEffect();
    }

    private VideoEffectProfile getVideoProfile() {
        return previewVideoEffect ? VideoEffectProfile.off() : VideoSetting.getAppliedProfile();
    }

    private AudioEffectParameters getAudioConfig(int channelCount) {
        return previewAudioEffect ? AudioEffectParameters.DEFAULT : AudioEffectConfig.from(getAudioEffectBands(), channelCount);
    }

    private void applyAudioConfig(int channelCount) {
        if (channelCount == Format.NO_VALUE) return;
        AudioEffectParameters config = getAudioConfig(channelCount);
        audioEffectFailed = !player.setAudioFilter(MpvAudioEffectFilter.create(config, channelCount));
    }
}
