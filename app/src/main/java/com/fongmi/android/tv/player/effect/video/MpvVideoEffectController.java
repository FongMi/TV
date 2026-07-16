package com.fongmi.android.tv.player.effect.video;

import androidx.media3.mpvplayer.MpvPlayer;
import androidx.media3.mpvplayer.video.MpvVideoEqualizer;

public final class MpvVideoEffectController {

    public void apply(MpvPlayer player, VideoEffectProfile profile) {
        player.setVideoEqualizer(toVideoEqualizer(profile));
    }

    public void clear(MpvPlayer player) {
        player.setVideoEqualizer(MpvVideoEqualizer.DEFAULT);
    }

    private MpvVideoEqualizer toVideoEqualizer(VideoEffectProfile profile) {
        float gamma = toGamma(profile.getGamma());
        float contrast = convert(profile.getContrast(), 1.0f);
        float brightness = convert(profile.getBrightness(), 0.0f);
        float saturation = convert(profile.getSaturation(), 1.0f);
        float sharpness = Math.clamp(profile.getSharpness(), 0.0f, 1.0f);
        return MpvVideoEqualizer.create(brightness, contrast, saturation, gamma, toHue(profile.getHue()), sharpness);
    }

    private float toGamma(float gamma) {
        return Math.clamp((float) (Math.log(gamma) * 100.0 / Math.log(8.0)), -100.0f, 100.0f);
    }

    private float convert(float value, float neutral) {
        return Math.clamp((value - neutral) * 100.0f, -100.0f, 100.0f);
    }

    private float toHue(float hue) {
        return Math.clamp(hue / 1.8f, -100.0f, 100.0f);
    }
}
