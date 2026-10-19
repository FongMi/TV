package com.fongmi.android.tv.setting;

import androidx.media3.common.audio.AudioChannelMode;

import com.fongmi.android.tv.player.effect.audio.AudioEffectBands;
import com.fongmi.android.tv.player.effect.audio.AudioEffectPreset;
import com.fongmi.android.tv.player.effect.audio.AudioPresetLevels;
import com.github.catvod.utils.Prefers;

public class AudioSetting {

    public static final int MIN_STABILITY = 0;
    public static final int MAX_STABILITY = 100;
    public static final int MIN_DIALOGUE = 0;
    public static final int MAX_DIALOGUE = 100;
    public static final int MIN_BOOST = 0;
    public static final int MAX_BOOST = 2000;
    public static final int MIN_PREAMP = -1200;
    public static final int MAX_PREAMP = 0;
    public static final int MIN_CENTER_GAIN = 0;
    public static final int MAX_CENTER_GAIN = 1200;
    public static final int MIN_BALANCE = -100;
    public static final int MAX_BALANCE = 100;

    public static boolean isEnabled() {
        return Prefers.getBoolean("audio_enabled");
    }

    public static boolean hasEffect(int channelCount) {
        int channelMode = AudioChannelMode.resolve(getChannelMode(), channelCount);
        boolean centerGain = getCenterGain() != 0 && (channelCount == 6 || channelCount == 8);
        boolean balance = AudioChannelMode.canBalance(channelMode) && getBalance() != 0;
        boolean channelEffect = AudioChannelMode.isAvailable(channelMode, channelCount) && (!AudioChannelMode.isAuto(channelMode) || balance);
        return isEnabled() || getStability() != 0 || getDialogue() != 0 || getBoost() != 0 || getPreamp() != 0 || centerGain || isLoudnessEnabled() || channelEffect;
    }

    public static int getPreset() {
        return AudioEffectPreset.clamp(Prefers.getInt("audio_preset", AudioEffectPreset.NATURAL));
    }

    public static void putPreset(int preset) {
        int value = AudioEffectPreset.clamp(preset);
        Prefers.put("audio_enabled", value != AudioEffectPreset.OFF);
        if (value != AudioEffectPreset.OFF) Prefers.put("audio_preset", value);
    }

    public static short[] getLevels(AudioEffectBands bands) {
        if (getPreset() == AudioEffectPreset.CUSTOM) return getCustomLevels(bands);
        return AudioPresetLevels.of(getPreset(), bands);
    }

    public static short[] getAppliedLevels(AudioEffectBands bands) {
        short[] levels = isEnabled() ? getLevels(bands) : AudioPresetLevels.of(AudioEffectPreset.OFF, bands);
        for (int i = 0; i < levels.length; i++) {
            float level = levels[i] + getDialogueLevel(bands.getCenterFrequency(i));
            levels[i] = bands.clamp(Math.round(level));
        }
        return levels;
    }

    public static short[] getCustomLevels(AudioEffectBands bands) {
        short[] levels = AudioPresetLevels.of(AudioEffectPreset.OFF, bands);
        String[] values = Prefers.getString(getCustomLevelsKey(bands)).split(",");
        if (values.length != bands.getCount()) return levels;
        for (int i = 0; i < values.length; i++) levels[i] = parseLevel(bands, values[i]);
        return levels;
    }

    private static short parseLevel(AudioEffectBands bands, String value) {
        try {
            return bands.clamp(Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static void putCustomLevels(AudioEffectBands bands, short[] levels) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < levels.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(levels[i]);
        }
        Prefers.put(getCustomLevelsKey(bands), builder.toString());
    }

    public static int getStability() {
        return Math.clamp(Prefers.getInt("audio_stability"), MIN_STABILITY, MAX_STABILITY);
    }

    public static void putStability(int stability) {
        Prefers.put("audio_stability", Math.clamp(stability, MIN_STABILITY, MAX_STABILITY));
    }

    public static int getDialogue() {
        return Math.clamp(Prefers.getInt("audio_dialogue"), MIN_DIALOGUE, MAX_DIALOGUE);
    }

    public static void putDialogue(int dialogue) {
        Prefers.put("audio_dialogue", Math.clamp(dialogue, MIN_DIALOGUE, MAX_DIALOGUE));
    }

    public static int getBoost() {
        return Math.clamp(Prefers.getInt("audio_boost"), MIN_BOOST, MAX_BOOST);
    }

    public static void putBoost(int boost) {
        Prefers.put("audio_boost", Math.clamp(boost, MIN_BOOST, MAX_BOOST));
    }

    public static int getPreamp() {
        return Math.clamp(Prefers.getInt("audio_preamp"), MIN_PREAMP, MAX_PREAMP);
    }

    public static void putPreamp(int preamp) {
        Prefers.put("audio_preamp", Math.clamp(preamp, MIN_PREAMP, MAX_PREAMP));
    }

    public static boolean isLoudnessEnabled() {
        return Prefers.getBoolean("audio_loudness");
    }

    public static void putLoudness(boolean enabled) {
        Prefers.put("audio_loudness", enabled);
    }

    public static int getCenterGain() {
        return Math.clamp(Prefers.getInt("audio_center_gain"), MIN_CENTER_GAIN, MAX_CENTER_GAIN);
    }

    public static void putCenterGain(int gain) {
        Prefers.put("audio_center_gain", Math.clamp(gain, MIN_CENTER_GAIN, MAX_CENTER_GAIN));
    }

    public static int getBalance() {
        return Math.clamp(Prefers.getInt("audio_balance"), MIN_BALANCE, MAX_BALANCE);
    }

    public static void putBalance(int balance) {
        Prefers.put("audio_balance", Math.clamp(balance, MIN_BALANCE, MAX_BALANCE));
    }

    public static int getChannelMode() {
        return AudioChannelMode.clamp(Prefers.getInt("audio_channel_mode", AudioChannelMode.AUTO));
    }

    public static void putChannelMode(int mode) {
        Prefers.put("audio_channel_mode", AudioChannelMode.clamp(mode));
    }

    public static void reset() {
        Prefers.put("audio_enabled", false);
        Prefers.put("audio_preset", AudioEffectPreset.NATURAL);
        Prefers.put(getCustomLevelsKey(AudioEffectBands.STANDARD), "");
        Prefers.put(getCustomLevelsKey(AudioEffectBands.EMPTY), "");
        resetAdvanced();
    }

    public static void resetAdvanced() {
        putStability(0);
        putDialogue(0);
        putBoost(0);
        putPreamp(0);
        putLoudness(false);
        putCenterGain(0);
        putBalance(0);
        putChannelMode(AudioChannelMode.AUTO);
    }

    private static String getCustomLevelsKey(AudioEffectBands bands) {
        if (bands == null || bands.isEmpty()) return "audio_custom_levels";
        StringBuilder builder = new StringBuilder("audio_custom_levels");
        builder.append('_').append(bands.getMinLevel()).append('_').append(bands.getMaxLevel());
        for (int i = 0; i < bands.getCount(); i++) builder.append('_').append(bands.getCenterFrequency(i));
        return builder.toString();
    }

    private static float getDialogueLevel(int milliHz) {
        int dialogue = getDialogue();
        if (dialogue == 0) return 0.0f;
        int hz = milliHz / 1000;
        if (hz <= 0) return 0.0f;
        if (hz < 180) return -180.0f * dialogue / MAX_DIALOGUE;
        if (hz < 500) return -80.0f * dialogue / MAX_DIALOGUE;
        double octaves = Math.abs(Math.log(hz / 2500.0) / Math.log(2.0));
        double weight = Math.max(0.0, 1.0 - octaves / 2.0);
        return (float) (weight * 650.0 * dialogue / MAX_DIALOGUE);
    }
}
