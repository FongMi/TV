package com.fongmi.android.tv.player.effect.video;

public final class VideoEffectPreset {

    public static final int OFF = 0;
    public static final int NATURAL = 1;
    public static final int VIVID = 2;
    public static final int CLEAR = 3;
    public static final int BRIGHT = 4;
    public static final int CINEMA = 5;
    public static final int SOFT = 6;
    public static final int WARM = 7;
    public static final int COOL = 8;
    public static final int COMFORT = 9;
    public static final int ANIME = 10;
    public static final int SPORT = 11;
    public static final int GAME = 12;
    public static final int CUSTOM = 13;

    public static int clamp(int preset) {
        return Math.clamp(preset, OFF, CUSTOM);
    }
}
