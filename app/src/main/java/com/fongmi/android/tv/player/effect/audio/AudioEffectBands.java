package com.fongmi.android.tv.player.effect.audio;

import java.util.Arrays;

public final class AudioEffectBands {

    public static final AudioEffectBands EMPTY = new AudioEffectBands((short) 0, (short) 0, new int[0]);
    public static final AudioEffectBands STANDARD = new AudioEffectBands((short) -1200, (short) 1200, new int[]{32_000, 64_000, 125_000, 250_000, 500_000, 1_000_000, 2_000_000, 4_000_000, 8_000_000, 16_000_000});

    private final short minLevel;
    private final short maxLevel;
    private final int[] centerFrequencies;

    public AudioEffectBands(short minLevel, short maxLevel, int[] centerFrequencies) {
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.centerFrequencies = centerFrequencies == null ? new int[0] : Arrays.copyOf(centerFrequencies, centerFrequencies.length);
    }

    public boolean isEmpty() {
        return centerFrequencies.length == 0 || minLevel >= maxLevel;
    }

    public int getCount() {
        return centerFrequencies.length;
    }

    public short getMinLevel() {
        return minLevel;
    }

    public short getMaxLevel() {
        return maxLevel;
    }

    public int getCenterFrequency(int index) {
        return centerFrequencies[index];
    }

    public short clamp(int level) {
        return (short) Math.clamp(level, minLevel, maxLevel);
    }

    public short snapToStep(int level, int stepSize) {
        int clamped = clamp(level);
        if (stepSize <= 0) return (short) clamped;
        int steps = Math.round((clamped - minLevel) / (float) stepSize);
        return clamp(minLevel + steps * stepSize);
    }
}
