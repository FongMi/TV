package com.fongmi.android.tv.player.effect.video;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.effect.GlEffect;
import androidx.media3.effect.GlShaderProgram;

final class ToneAdjustEffect implements GlEffect {

    private volatile VideoEffectProfile profile;

    ToneAdjustEffect() {
        setProfile(VideoEffectProfile.off());
    }

    void setProfile(VideoEffectProfile profile) {
        this.profile = profile;
    }

    float getGamma() {
        return profile.gamma;
    }

    float getHue() {
        return profile.hue;
    }

    @NonNull
    @Override
    public GlShaderProgram toGlShaderProgram(@NonNull Context context, boolean useHdr) throws VideoFrameProcessingException {
        return new ToneAdjustShaderProgram(useHdr, this);
    }

    @Override
    public boolean isNoOp(int inputWidth, int inputHeight) {
        return profile.isToneNoOp();
    }
}
