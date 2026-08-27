package com.fongmi.android.tv.setting;

import com.github.catvod.utils.Prefers;
import com.google.android.material.slider.Slider;

import java.util.Locale;

public class SpeedSetting {

    public static final float MIN = 0.1f;
    public static final float MAX = 5.0f;
    public static final float STEP = 0.1f;
    public static final float NORMAL = 1.0f;
    public static final float LONG_PRESS = 2.0f;
    private static final float LONG_PRESS_MIN = 2.0f;
    private static final float LONG_PRESS_STEP = 0.5f;
    private static final float EPSILON = 0.001f;
    private static final float[] PRESETS = {0.5f, 0.8f, 1.0f, 1.2f, 1.5f, 2.0f, 3.0f, 5.0f};

    public static void setup(Slider slider) {
        slider.setValueFrom(MIN);
        slider.setValueTo(MAX);
        slider.setStepSize(STEP);
        slider.setLabelFormatter(SpeedSetting::format);
    }

    public static void setupLongPress(Slider slider) {
        slider.setValueFrom(LONG_PRESS_MIN);
        slider.setValueTo(MAX);
        slider.setStepSize(LONG_PRESS_STEP);
        slider.setLabelFormatter(SpeedSetting::format);
    }

    public static float clamp(float speed) {
        return Math.clamp(speed, MIN, MAX);
    }

    public static float clampLongPress(float speed) {
        return Math.clamp(speed, LONG_PRESS_MIN, MAX);
    }

    public static float[] getPresets() {
        return PRESETS.clone();
    }

    public static float getPlayback() {
        return clamp(Prefers.getFloat("speed_playback", NORMAL));
    }

    public static void putPlayback(float speed) {
        Prefers.put("speed_playback", clamp(speed));
    }

    public static float getLongPress() {
        return clampLongPress(Prefers.getFloat("speed_long_press", Prefers.getFloat("speed", LONG_PRESS)));
    }

    public static void putLongPress(float speed) {
        Prefers.put("speed_long_press", clampLongPress(speed));
    }

    public static boolean isSkipSilence() {
        return Prefers.getBoolean("speed_skip_silence");
    }

    public static void putSkipSilence(boolean enabled) {
        Prefers.put("speed_skip_silence", enabled);
    }

    public static float next(float speed) {
        float value = clamp(speed);
        for (float preset : PRESETS) if (preset > value + EPSILON) return preset;
        return MIN;
    }

    public static String format(float speed) {
        return formatValue(speed) + "x";
    }

    public static String formatValue(float speed) {
        float value = clamp(speed);
        return formatRawValue(value);
    }

    private static String formatRawValue(float value) {
        return isSingleDecimal(value) ? String.format(Locale.getDefault(), "%.1f", value) : String.format(Locale.getDefault(), "%.2f", value);
    }

    private static boolean isSingleDecimal(float value) {
        return Math.abs(value * 10 - Math.round(value * 10)) < EPSILON;
    }
}
