package com.fongmi.android.tv.player.effect.audio;

import androidx.media3.common.audio.AudioChannelMode;
import androidx.media3.common.audio.AudioEffectParameters;

import com.fongmi.android.tv.setting.AudioSetting;

import java.util.stream.IntStream;

public final class AudioEffectConfig {

    public static AudioEffectParameters from(AudioEffectBands bands, int channelCount) {
        int channelMode = AudioChannelMode.resolve(AudioSetting.getChannelMode(), channelCount);
        int centerGain = isCenterGainAvailable(channelCount) ? AudioSetting.getCenterGain() : 0;
        int balance = AudioChannelMode.isAvailable(channelMode, channelCount) ? AudioSetting.getBalance() : 0;
        boolean hasEqualizer = AudioSetting.isEnabled() || AudioSetting.getDialogue() != 0;
        short[] levels = hasEqualizer && !bands.isEmpty() ? AudioSetting.getAppliedLevels(bands) : new short[0];
        int preamp = getEffectivePreamp(levels);
        int[] frequencies = IntStream.range(0, levels.length).map(i -> bands.getCenterFrequency(i) / 1000).toArray();
        return new AudioEffectParameters.Builder().setEqualizerBands(frequencies, levels)
                .setStability(Math.clamp(AudioSetting.getStability(), AudioSetting.MIN_STABILITY, AudioSetting.MAX_STABILITY) / (float) AudioSetting.MAX_STABILITY)
                .setBoostMillibels(Math.clamp(AudioSetting.getBoost(), AudioSetting.MIN_BOOST, AudioSetting.MAX_BOOST))
                .setPreampMillibels(preamp)
                .setCenterGainMillibels(Math.clamp(centerGain, AudioSetting.MIN_CENTER_GAIN, AudioSetting.MAX_CENTER_GAIN))
                .setBalance(Math.clamp(balance, AudioSetting.MIN_BALANCE, AudioSetting.MAX_BALANCE) / 100.0f)
                .setChannelMode(channelMode).setLoudnessEnabled(AudioSetting.isLoudnessEnabled()).build();
    }

    public static boolean isCenterGainAvailable(int channelCount) {
        return AudioEffectParameters.isCenterGainAvailable(channelCount);
    }

    private static int getEffectivePreamp(short[] levels) {
        int preamp = Math.clamp(AudioSetting.getPreamp(), AudioSetting.MIN_PREAMP, AudioSetting.MAX_PREAMP);
        int maxLevel = 0;
        for (short level : levels) maxLevel = Math.max(maxLevel, level);
        return preamp - maxLevel;
    }
}
