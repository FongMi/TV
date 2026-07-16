package com.fongmi.android.tv.player.effect.audio;

public final class AudioPresetLevels {

    public static short[] of(int preset, AudioEffectBands bands) {
        short[] levels = new short[bands.getCount()];
        for (int i = 0; i < levels.length; i++) {
            int hz = bands.getCenterFrequency(i) / 1000;
            levels[i] = bands.clamp(gainFor(preset, hz));
        }
        return levels;
    }

    private static int gainFor(int preset, int hz) {
        return switch (AudioEffectPreset.clamp(preset)) {
            case AudioEffectPreset.NATURAL -> natural(hz);
            case AudioEffectPreset.VOCAL -> vocal(hz);
            case AudioEffectPreset.CINEMA -> cinema(hz);
            case AudioEffectPreset.BASS -> bass(hz);
            case AudioEffectPreset.TREBLE -> treble(hz);
            case AudioEffectPreset.POP -> pop(hz);
            case AudioEffectPreset.ROCK -> rock(hz);
            case AudioEffectPreset.DANCE -> dance(hz);
            case AudioEffectPreset.ELECTRONIC -> electronic(hz);
            case AudioEffectPreset.HIPHOP -> hiphop(hz);
            case AudioEffectPreset.JAZZ -> jazz(hz);
            case AudioEffectPreset.CLASSICAL -> classical(hz);
            default -> 0;
        };
    }

    private static int natural(int hz) {
        if (hz < 160) return 80;
        if (hz < 500) return 40;
        if (hz < 2000) return 0;
        if (hz < 6000) return 80;
        return 60;
    }

    private static int pop(int hz) {
        if (hz < 160) return 260;
        if (hz < 500) return 100;
        if (hz < 2000) return 80;
        if (hz < 6000) return 280;
        return 220;
    }

    private static int rock(int hz) {
        if (hz < 160) return 380;
        if (hz < 600) return 140;
        if (hz < 2500) return 60;
        if (hz < 7000) return 340;
        return 260;
    }

    private static int classical(int hz) {
        if (hz < 160) return 40;
        if (hz < 800) return 100;
        if (hz < 3000) return 140;
        if (hz < 9000) return 220;
        return 100;
    }

    private static int cinema(int hz) {
        if (hz < 120) return 460;
        if (hz < 500) return 220;
        if (hz < 2500) return -80;
        if (hz < 7000) return 180;
        return 300;
    }

    private static int vocal(int hz) {
        if (hz < 160) return -180;
        if (hz < 600) return -80;
        if (hz < 1500) return 140;
        if (hz < 5000) return 340;
        return 100;
    }

    private static int bass(int hz) {
        if (hz < 120) return 600;
        if (hz < 300) return 460;
        if (hz < 700) return 180;
        if (hz < 2500) return -120;
        return -40;
    }

    private static int jazz(int hz) {
        if (hz < 120) return 140;
        if (hz < 500) return 160;
        if (hz < 1800) return 120;
        if (hz < 6000) return 260;
        return 140;
    }

    private static int dance(int hz) {
        if (hz < 120) return 560;
        if (hz < 300) return 400;
        if (hz < 1200) return -160;
        if (hz < 5000) return 220;
        return 400;
    }

    private static int electronic(int hz) {
        if (hz < 120) return 440;
        if (hz < 500) return 140;
        if (hz < 2200) return -120;
        if (hz < 7000) return 280;
        return 520;
    }

    private static int hiphop(int hz) {
        if (hz < 120) return 600;
        if (hz < 500) return 340;
        if (hz < 2000) return -100;
        if (hz < 6000) return 120;
        return 220;
    }

    private static int treble(int hz) {
        if (hz < 160) return -180;
        if (hz < 700) return -80;
        if (hz < 2200) return 60;
        if (hz < 7000) return 340;
        return 520;
    }
}
