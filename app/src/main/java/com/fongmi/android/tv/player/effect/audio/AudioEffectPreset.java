package com.fongmi.android.tv.player.effect.audio;

public final class AudioEffectPreset {

    public static final int OFF = 0;
    public static final int NATURAL = 1;
    public static final int VOCAL = 2;
    public static final int CINEMA = 3;
    public static final int BASS = 4;
    public static final int TREBLE = 5;
    public static final int POP = 6;
    public static final int ROCK = 7;
    public static final int DANCE = 8;
    public static final int ELECTRONIC = 9;
    public static final int HIPHOP = 10;
    public static final int JAZZ = 11;
    public static final int CLASSICAL = 12;
    public static final int CUSTOM = 13;

    public static int clamp(int preset) {
        return Math.clamp(preset, OFF, CUSTOM);
    }
}
