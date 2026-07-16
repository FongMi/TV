package com.fongmi.android.tv.player.effect;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.player.effect.audio.AudioEffectBands;

public interface PlayerEffect {

    PlayerEffect NONE = new PlayerEffect() {
    };

    default boolean supportsVideoEffect() {
        return false;
    }

    default int getVideoEffectError() {
        return R.string.error_video_effect_unsupported;
    }

    default boolean supportsVideoSharpness() {
        return true;
    }

    default void applyVideoEffect() {
    }

    default void previewVideoEffect(boolean original) {
    }

    default boolean supportsAudioEffect() {
        return false;
    }

    default AudioEffectBands getAudioEffectBands() {
        return AudioEffectBands.EMPTY;
    }

    default int getAudioEffectError() {
        return R.string.error_audio_effect_unsupported;
    }

    default void applyAudioEffect() {
    }

    default void previewAudioEffect(boolean original) {
    }

    default boolean supportsSkipSilence() {
        return false;
    }

    default void setSkipSilenceEnabled(boolean enabled) {
    }
}
