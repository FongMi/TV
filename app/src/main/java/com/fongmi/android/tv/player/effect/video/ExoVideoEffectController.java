package com.fongmi.android.tv.player.effect.video;

import androidx.media3.common.Effect;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.List;

public final class ExoVideoEffectController {

    private final ColorToneAdjustEffect colorTone;
    private final DetailAdjustEffect detail;
    private final List<Effect> effects;
    private boolean configured;

    public ExoVideoEffectController() {
        this.colorTone = new ColorToneAdjustEffect();
        this.detail = new DetailAdjustEffect();
        this.effects = List.of(colorTone, detail);
    }

    public void apply(ExoPlayer player, VideoEffectProfile profile) {
        if (!configured && profile.isNoOp()) return;
        colorTone.setProfile(profile);
        detail.setProfile(profile);
        player.setVideoEffects(effects);
        configured = true;
    }

    public void clear(ExoPlayer player) {
        if (!configured) return;
        player.setVideoEffects(List.of());
        configured = false;
    }
}
