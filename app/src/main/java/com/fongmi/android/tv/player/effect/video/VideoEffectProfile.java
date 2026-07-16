package com.fongmi.android.tv.player.effect.video;

public final class VideoEffectProfile {

    private static final VideoEffectProfile OFF = custom(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f);
    private static final float DEFAULT_THRESHOLD = 0.03f;

    final float saturation;
    final float contrast;
    final float brightness;
    final float gamma;
    final float hue;
    final float temperature;
    final float sharpness;
    final float threshold;
    final float shadowLift;

    private VideoEffectProfile(float saturation, float contrast, float brightness, float gamma, float hue, float temperature, float sharpness, float threshold, float shadowLift) {
        this.saturation = saturation;
        this.contrast = contrast;
        this.brightness = brightness;
        this.gamma = gamma;
        this.hue = hue;
        this.temperature = temperature;
        this.sharpness = sharpness;
        this.threshold = threshold;
        this.shadowLift = shadowLift;
    }

    public static VideoEffectProfile off() {
        return OFF;
    }

    public static VideoEffectProfile custom(float saturation, float contrast, float brightness, float sharpness, float shadowLift, float gamma, float hue, float temperature) {
        return new VideoEffectProfile(saturation, contrast, brightness, gamma, hue, temperature, sharpness, DEFAULT_THRESHOLD, shadowLift);
    }

    public static VideoEffectProfile of(int preset) {
        return switch (VideoEffectPreset.clamp(preset)) {
            case VideoEffectPreset.NATURAL -> basic(1.02f, 1.02f, 0.0f, 0.04f, 0.035f, 0.0f);
            case VideoEffectPreset.VIVID -> basic(1.26f, 1.12f, 0.01f, 0.14f, 0.035f, 0.0f);
            case VideoEffectPreset.CLEAR -> basic(1.04f, 1.12f, 0.0f, 0.36f, 0.025f, 0.02f);
            case VideoEffectPreset.BRIGHT -> style(1.04f, 1.05f, 0.01f, 0.04f, 0.06f, 1.01f, 0.0f);
            case VideoEffectPreset.CINEMA -> style(1.04f, 1.14f, -0.03f, 0.03f, 0.03f, 0.97f, 26.0f);
            case VideoEffectPreset.SOFT -> style(0.95f, 0.94f, 0.005f, 0.0f, 0.05f, 1.03f, 14.0f);
            case VideoEffectPreset.WARM -> style(1.05f, 1.04f, 0.0f, 0.03f, 0.02f, 1.0f, 42.0f);
            case VideoEffectPreset.COOL -> style(1.04f, 1.05f, 0.0f, 0.03f, 0.02f, 1.0f, -42.0f);
            case VideoEffectPreset.COMFORT -> style(0.92f, 0.93f, -0.01f, 0.0f, 0.06f, 1.04f, 58.0f);
            case VideoEffectPreset.ANIME -> basic(1.24f, 1.10f, 0.02f, 0.28f, 0.025f, 0.02f);
            case VideoEffectPreset.SPORT -> basic(1.12f, 1.14f, 0.02f, 0.24f, DEFAULT_THRESHOLD, 0.04f);
            case VideoEffectPreset.GAME -> basic(1.08f, 1.14f, 0.02f, 0.30f, 0.025f, 0.04f);
            case VideoEffectPreset.OFF, VideoEffectPreset.CUSTOM -> off();
            default -> throw new IllegalArgumentException();
        };
    }

    private static VideoEffectProfile basic(float saturation, float contrast, float brightness, float sharpness, float threshold, float shadowLift) {
        return new VideoEffectProfile(saturation, contrast, brightness, 1.0f, 0.0f, 0.0f, sharpness, threshold, shadowLift);
    }

    private static VideoEffectProfile style(float saturation, float contrast, float brightness, float sharpness, float shadowLift, float gamma, float temperature) {
        return custom(saturation, contrast, brightness, sharpness, shadowLift, gamma, 0.0f, temperature);
    }

    float redGain() {
        return temperature >= 0.0f ? 1.0f + temperature * 0.0015f : 1.0f + temperature * 0.0012f;
    }

    float blueGain() {
        return temperature >= 0.0f ? 1.0f - temperature * 0.0012f : 1.0f - temperature * 0.0015f;
    }

    public float getSaturation() {
        return saturation;
    }

    public float getContrast() {
        return contrast;
    }

    public float getBrightness() {
        return brightness;
    }

    public float getSharpness() {
        return sharpness;
    }

    public float getShadowLift() {
        return shadowLift;
    }

    public float getGamma() {
        return gamma;
    }

    public float getHue() {
        return hue;
    }

    public float getTemperature() {
        return temperature;
    }

    boolean isNoOp() {
        return isColorNoOp() && isToneNoOp() && isDetailNoOp();
    }

    boolean isColorNoOp() {
        return saturation == 1.0f && contrast == 1.0f && brightness == 0.0f && redGain() == 1.0f && blueGain() == 1.0f;
    }

    boolean isToneNoOp() {
        return gamma == 1.0f && hue == 0.0f;
    }

    boolean isDetailNoOp() {
        return sharpness == 0.0f && shadowLift == 0.0f;
    }
}
