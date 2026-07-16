package com.fongmi.android.tv.player.effect.audio;

import android.os.Build;

import androidx.media3.common.audio.AudioEffectParameters;
import androidx.media3.common.audio.AudioEffectProcessor;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.audio.AudioEqualizerController;

import com.fongmi.android.tv.App;

public final class ExoAudioEffectController {

    private final AudioEqualizerController equalizer;
    private final AudioEffectProcessor processor;

    public ExoAudioEffectController() {
        this.equalizer = new AudioEqualizerController(App.get());
        this.processor = new AudioEffectProcessor();
    }

    public AudioEffectProcessor getProcessor() {
        return processor;
    }

    public boolean apply(ExoPlayer player, AudioEffectParameters config) {
        if (config.hasEffect()) return applyEffect(player, config);
        release();
        return true;
    }

    private boolean applyEffect(ExoPlayer player, AudioEffectParameters config) {
        boolean softwareEqualizer = config.hasBands() && Build.VERSION.SDK_INT < Build.VERSION_CODES.P;
        boolean success = applyEqualizer(player, config, softwareEqualizer);
        if (success) processor.setParameters(config, softwareEqualizer);
        else processor.resetParameters();
        return success;
    }

    private boolean applyEqualizer(ExoPlayer player, AudioEffectParameters config, boolean softwareEqualizer) {
        if (!softwareEqualizer && config.hasBands()) return equalizer.apply(player, config);
        equalizer.release();
        return true;
    }

    public void release() {
        equalizer.release();
        processor.resetParameters();
    }
}
