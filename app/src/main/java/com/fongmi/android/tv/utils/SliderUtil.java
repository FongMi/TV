package com.fongmi.android.tv.utils;

import com.google.android.material.slider.Slider;

public class SliderUtil {

    private static final float STEP_TOLERANCE = 1e-4f;

    public static void setValue(Slider slider, float value) {
        slider.setValue(snap(slider, value));
    }

    public static float snap(Slider slider, float value) {
        return snap(value, slider.getValueFrom(), slider.getValueTo(), slider.getStepSize());
    }

    public static float snap(float value, float from, float to, float step) {
        float clamped = Math.clamp(value, from, to);
        if (step <= 0) return clamped;
        float snapped = from + Math.round((clamped - from) / step) * step;
        if (from <= 0.0f && to >= 0.0f && Math.abs(snapped) <= step * STEP_TOLERANCE) snapped = 0.0f;
        return Math.clamp(snapped, from, to);
    }
}
