package com.fongmi.android.tv.setting;

import com.fongmi.android.tv.player.effect.video.VideoEffectPreset;
import com.fongmi.android.tv.player.effect.video.VideoEffectProfile;
import com.github.catvod.utils.Prefers;

public class VideoSetting {

    public static final float MIN_SATURATION = 0.5f;
    public static final float MAX_SATURATION = 2.0f;
    public static final float MIN_CONTRAST = 0.5f;
    public static final float MAX_CONTRAST = 1.8f;
    public static final float MIN_BRIGHTNESS = -0.2f;
    public static final float MAX_BRIGHTNESS = 0.2f;
    public static final float MIN_SHARPNESS = 0.0f;
    public static final float MAX_SHARPNESS = 0.8f;
    public static final float MIN_SHADOW = 0.0f;
    public static final float MAX_SHADOW = 0.6f;
    public static final float MIN_GAMMA = 0.5f;
    public static final float MAX_GAMMA = 2.0f;
    public static final float MIN_HUE = -180.0f;
    public static final float MAX_HUE = 180.0f;
    public static final float MIN_TEMPERATURE = -100.0f;
    public static final float MAX_TEMPERATURE = 100.0f;

    public static boolean isEnabled() {
        return Prefers.getBoolean("video_enabled");
    }

    public static int getPreset() {
        return VideoEffectPreset.clamp(Prefers.getInt("video_preset", VideoEffectPreset.NATURAL));
    }

    public static void putPreset(int preset) {
        int value = VideoEffectPreset.clamp(preset);
        Prefers.put("video_enabled", value != VideoEffectPreset.OFF);
        if (value != VideoEffectPreset.OFF) Prefers.put("video_preset", value);
    }

    public static VideoEffectProfile getProfile() {
        int preset = getPreset();
        return preset == VideoEffectPreset.CUSTOM ? getCustomProfile() : VideoEffectProfile.of(preset);
    }

    public static VideoEffectProfile getAppliedProfile() {
        return isEnabled() ? getProfile() : VideoEffectProfile.off();
    }

    public static VideoEffectProfile getCustomProfile() {
        return VideoEffectProfile.custom(getSaturation(), getContrast(), getBrightness(), getSharpness(), getShadow(), getGamma(), getHue(), getTemperature());
    }

    public static float getSaturation() {
        return Math.clamp(Prefers.getFloat("video_saturation", 1.0f), MIN_SATURATION, MAX_SATURATION);
    }

    public static void putSaturation(float value) {
        Prefers.put("video_saturation", Math.clamp(value, MIN_SATURATION, MAX_SATURATION));
    }

    public static float getContrast() {
        return Math.clamp(Prefers.getFloat("video_contrast", 1.0f), MIN_CONTRAST, MAX_CONTRAST);
    }

    public static void putContrast(float value) {
        Prefers.put("video_contrast", Math.clamp(value, MIN_CONTRAST, MAX_CONTRAST));
    }

    public static float getBrightness() {
        return Math.clamp(Prefers.getFloat("video_brightness"), MIN_BRIGHTNESS, MAX_BRIGHTNESS);
    }

    public static void putBrightness(float value) {
        Prefers.put("video_brightness", Math.clamp(value, MIN_BRIGHTNESS, MAX_BRIGHTNESS));
    }

    public static float getSharpness() {
        return Math.clamp(Prefers.getFloat("video_sharpness"), MIN_SHARPNESS, MAX_SHARPNESS);
    }

    public static void putSharpness(float value) {
        Prefers.put("video_sharpness", Math.clamp(value, MIN_SHARPNESS, MAX_SHARPNESS));
    }

    public static float getShadow() {
        return Math.clamp(Prefers.getFloat("video_shadow"), MIN_SHADOW, MAX_SHADOW);
    }

    public static void putShadow(float value) {
        Prefers.put("video_shadow", Math.clamp(value, MIN_SHADOW, MAX_SHADOW));
    }

    public static float getGamma() {
        return Math.clamp(Prefers.getFloat("video_gamma", 1.0f), MIN_GAMMA, MAX_GAMMA);
    }

    public static void putGamma(float value) {
        Prefers.put("video_gamma", Math.clamp(value, MIN_GAMMA, MAX_GAMMA));
    }

    public static float getHue() {
        return Math.clamp(Prefers.getFloat("video_hue"), MIN_HUE, MAX_HUE);
    }

    public static void putHue(float value) {
        Prefers.put("video_hue", Math.clamp(value, MIN_HUE, MAX_HUE));
    }

    public static float getTemperature() {
        return Math.clamp(Prefers.getFloat("video_temperature"), MIN_TEMPERATURE, MAX_TEMPERATURE);
    }

    public static void putTemperature(float value) {
        Prefers.put("video_temperature", Math.clamp(value, MIN_TEMPERATURE, MAX_TEMPERATURE));
    }

    public static void putCustomProfile(VideoEffectProfile profile) {
        putSaturation(profile.getSaturation());
        putContrast(profile.getContrast());
        putBrightness(profile.getBrightness());
        putSharpness(profile.getSharpness());
        putShadow(profile.getShadowLift());
        putGamma(profile.getGamma());
        putHue(profile.getHue());
        putTemperature(profile.getTemperature());
    }

    public static void reset() {
        putCustomProfile(VideoEffectProfile.off());
        Prefers.put("video_enabled", false);
        Prefers.put("video_preset", VideoEffectPreset.NATURAL);
    }
}
