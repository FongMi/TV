package com.fongmi.android.tv.player.effect.video;

import androidx.annotation.NonNull;
import androidx.media3.effect.RgbMatrix;

final class ColorAdjustEffect implements RgbMatrix {

    private static final float LUMA_R = 0.2126f;
    private static final float LUMA_G = 0.7152f;
    private static final float LUMA_B = 0.0722f;

    private volatile VideoEffectProfile profile;
    private volatile float[] matrix;

    ColorAdjustEffect() {
        setProfile(VideoEffectProfile.off());
    }

    private static float[] createMatrix(VideoEffectProfile profile) {
        float saturation = profile.saturation;
        float contrast = profile.contrast;
        float brightness = profile.brightness;
        float redGain = profile.redGain;
        float greenGain = profile.greenGain;
        float blueGain = profile.blueGain;
        float invSat = 1.0f - saturation;
        float offset = brightness + 0.5f * (1.0f - contrast);
        float rr = (LUMA_R * invSat + saturation) * contrast * redGain;
        float rg = (LUMA_G * invSat) * contrast * redGain;
        float rb = (LUMA_B * invSat) * contrast * redGain;
        float gr = (LUMA_R * invSat) * contrast * greenGain;
        float gg = (LUMA_G * invSat + saturation) * contrast * greenGain;
        float gb = (LUMA_B * invSat) * contrast * greenGain;
        float br = (LUMA_R * invSat) * contrast * blueGain;
        float bg = (LUMA_G * invSat) * contrast * blueGain;
        float bb = (LUMA_B * invSat + saturation) * contrast * blueGain;
        return new float[]{rr, gr, br, 0.0f, rg, gg, bg, 0.0f, rb, gb, bb, 0.0f, offset * redGain, offset * greenGain, offset * blueGain, 1.0f};
    }

    void setProfile(VideoEffectProfile profile) {
        this.profile = profile;
        this.matrix = createMatrix(profile);
    }

    @NonNull
    @Override
    public float[] getMatrix(long presentationTimeUs, boolean useHdr) {
        return matrix;
    }

    @Override
    public boolean isNoOp(int inputWidth, int inputHeight) {
        return profile.isColorNoOp();
    }
}
