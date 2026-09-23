package com.fongmi.android.tv.player.effect.audio;

import androidx.annotation.Nullable;
import androidx.media3.mpvplayer.audio.AudioChannelMix;
import androidx.media3.mpvplayer.audio.MpvAudioFilter;

public final class MpvAudioEffectFilter {

    public static MpvAudioFilter create(AudioEffectConfig config, int channelCount) {
        if (!config.hasEffect()) return MpvAudioFilter.EMPTY;
        MpvAudioFilter.Builder builder = new MpvAudioFilter.Builder();
        PanMix panMix = new PanMix(channelCount);
        boolean hasBands = config.hasBands();
        appendCenterGain(panMix, config, channelCount);
        appendChannelMode(panMix, config.getChannelMode(), channelCount);
        appendBalance(panMix, config.getBalance(), config.getChannelMode(), channelCount);
        panMix.appendTo(builder);
        appendLoudness(builder, config.isLoudnessEnabled());
        appendStability(builder, config);
        appendVolume(builder, "boost", config.getBoost(), hasBands);
        appendVolume(builder, "preamp", config.getPreamp(), hasBands);
        appendEqualizer(builder, config.getLevels());
        appendLimiter(builder, config.shouldLimitOutput(channelCount));
        return builder.build();
    }

    private static void appendCenterGain(PanMix panMix, AudioEffectConfig config, int channelCount) {
        if (!config.hasCenterGain(channelCount)) return;
        panMix.add(AudioChannelMix.createFrontCenterGainMix(channelCount, config.getCenterGainFactor()));
    }

    private static void appendChannelMode(PanMix panMix, int mode, int channelCount) {
        switch (AudioChannelMode.resolve(mode, channelCount)) {
            case AudioChannelMode.STEREO -> panMix.add(AudioChannelMix.createStereoMix(channelCount, false));
            case AudioChannelMode.MONO -> panMix.add(AudioChannelMix.createMonoMix(channelCount));
            case AudioChannelMode.REVERSE -> panMix.add(AudioChannelMix.createStereoMix(channelCount, true));
        }
    }

    private static void appendBalance(PanMix panMix, int balance, int mode, int channelCount) {
        if (balance == 0 || !AudioChannelMode.isAvailable(mode, channelCount)) return;
        panMix.add(AudioChannelMix.createFrontBalanceMix(channelCount, balance / 100.0f));
    }

    private static void appendLoudness(MpvAudioFilter.Builder builder, boolean enabled) {
        if (enabled) builder.addDynamicNormalization("loudness", 0.125, 4.0);
    }

    private static void appendStability(MpvAudioFilter.Builder builder, AudioEffectConfig config) {
        if (!config.hasBands() && config.getStability() <= 0) return;
        float amount = config.getStabilityAmount();
        float threshold = 0.25f - 0.08f * amount;
        float ratio = 1.4f + 2.6f * amount;
        float makeup = 1.0f + 0.8f * amount;
        float mix = config.getStability() > 0 ? 0.35f + 0.65f * amount : 0.0f;
        builder.addCompressor("stability", threshold, ratio, makeup, mix);
    }

    private static void appendVolume(MpvAudioFilter.Builder builder, String id, int level, boolean hasBands) {
        if (hasBands || level != 0) builder.addVolume(id, level / 100.0);
    }

    private static void appendEqualizer(MpvAudioFilter.Builder builder, short[] levels) {
        int count = Math.min(levels.length, AudioEffectBands.STANDARD.getCount());
        for (int i = 0; i < count; i++) builder.addEqualizer("eq" + i, AudioEffectBands.STANDARD.getCenterFrequency(i) / 1000, levels[i] / 100.0);
    }

    private static void appendLimiter(MpvAudioFilter.Builder builder, boolean enabled) {
        if (enabled) builder.addLimiter("limiter", 0.98);
    }

    private static final class PanMix {

        @Nullable
        private float[][] mix;

        private PanMix(int channelCount) {
            this.mix = channelCount >= 2 && channelCount <= 8 ? createIdentityMix(channelCount) : null;
        }

        private void add(float[][] next) {
            if (mix != null) mix = AudioChannelMix.compose(mix, next);
        }

        private void appendTo(MpvAudioFilter.Builder builder) {
            if (mix != null) builder.addRuntimeChannelMix("mix", mix);
        }

        private static float[][] createIdentityMix(int channelCount) {
            float[][] mix = new float[channelCount][channelCount];
            for (int channel = 0; channel < channelCount; channel++) mix[channel][channel] = 1.0f;
            return mix;
        }
    }
}
