package com.fongmi.android.tv.player.effect.video;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.effect.GlEffect;
import androidx.media3.effect.GlShaderProgram;

final class DetailAdjustEffect implements GlEffect {

    private volatile VideoEffectProfile profile;

    DetailAdjustEffect() {
        setProfile(VideoEffectProfile.off());
    }

    void setProfile(VideoEffectProfile profile) {
        this.profile = profile;
    }

    float getSharpness() {
        return profile.sharpness;
    }

    float getThreshold() {
        return profile.threshold;
    }

    float getShadowLift() {
        return profile.shadowLift;
    }

    float getShadowStart() {
        return profile.shadowStart;
    }

    float getShadowEnd() {
        return profile.shadowEnd;
    }

    @NonNull
    @Override
    public GlShaderProgram toGlShaderProgram(@NonNull Context context, boolean useHdr) throws VideoFrameProcessingException {
        return new DetailAdjustShaderProgram(useHdr, this);
    }

    @Override
    public boolean isNoOp(int inputWidth, int inputHeight) {
        return profile.isDetailNoOp();
    }
}
