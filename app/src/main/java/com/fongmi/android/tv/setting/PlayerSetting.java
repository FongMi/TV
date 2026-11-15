package com.fongmi.android.tv.setting;

import static androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL;
import static androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT;
import static androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.DecoderMode;

import com.github.catvod.utils.Prefers;
import com.github.catvod.utils.RuntimeModule;

public class PlayerSetting {

    public static final int ENGINE_EXO = 0;
    public static final int ENGINE_MPV = 1;
    public static final int MIN_SCALE = 0;
    public static final int MAX_SCALE = 2;
    private static final int MIN_SIZE = 0;
    private static final int MAX_SIZE = 3;
    private static final int MIN_BUFFER = 1;
    private static final int MAX_BUFFER = 10;
    private static final int MIN_BACKGROUND = 0;
    private static final int MAX_BACKGROUND = 2;

    public static int getEngine() {
        return Math.clamp(Prefers.getInt("player_engine", ENGINE_EXO), ENGINE_EXO, getEngineCount() - 1);
    }

    public static int getEngineCount() {
        return RuntimeModule.MPV.isInstalled() ? ENGINE_MPV + 1 : ENGINE_EXO + 1;
    }

    public static void putEngine(int engine) {
        Prefers.put("player_engine", Math.clamp(engine, ENGINE_EXO, ENGINE_MPV));
        if (isExo() && OutputSetting.isTunnel()) OutputSetting.putRender(OutputSetting.RENDER_SURFACE);
    }

    public static boolean isExo() {
        return getEngine() == ENGINE_EXO;
    }

    public static boolean isMpv() {
        return getEngine() == ENGINE_MPV;
    }

    public static DecoderMode getDecoderMode(int engine, @C.TrackType int trackType) {
        String key = getDecoderModeKey(engine, trackType);
        if (key == null) return DecoderMode.AUTO;
        String value = Prefers.getString(key);
        for (DecoderMode mode : DecoderMode.values()) if (mode.name().equals(value) && isDecoderModeSupported(engine, mode)) return mode;
        return DecoderMode.AUTO;
    }

    public static void putDecoderMode(int engine, @C.TrackType int trackType, DecoderMode mode) {
        String key = getDecoderModeKey(engine, trackType);
        if (key != null && isDecoderModeSupported(engine, mode)) Prefers.put(key, mode.name());
    }

    @Nullable
    private static String getDecoderModeKey(int engine, @C.TrackType int trackType) {
        if (engine == ENGINE_MPV) return trackType == C.TRACK_TYPE_VIDEO ? "decode_mpv_video_mode" : null;
        if (engine != ENGINE_EXO) return null;
        return switch (trackType) {
            case C.TRACK_TYPE_AUDIO -> "decode_exo_audio_mode";
            case C.TRACK_TYPE_VIDEO -> "decode_exo_video_mode";
            default -> null;
        };
    }

    private static boolean isDecoderModeSupported(int engine, DecoderMode mode) {
        return engine != ENGINE_MPV || mode != DecoderMode.SOFTWARE;
    }

    public static boolean isDebug() {
        return Prefers.getBoolean("player_debug");
    }

    public static void putDebug(boolean debug) {
        Prefers.put("player_debug", debug);
    }

    public static int getSize() {
        return Math.clamp(Prefers.getInt("size", 2), MIN_SIZE, MAX_SIZE);
    }

    public static void putSize(int size) {
        Prefers.put("size", Math.clamp(size, MIN_SIZE, MAX_SIZE));
    }

    public static int getScale() {
        return Math.clamp(Prefers.getInt("scale"), MIN_SCALE, MAX_SCALE);
    }

    public static void putScale(int scale) {
        Prefers.put("scale", Math.clamp(scale, MIN_SCALE, MAX_SCALE));
    }

    public static int getScaleMode(int index) {
        return index == 1 ? RESIZE_MODE_FILL : index == 2 ? RESIZE_MODE_ZOOM : RESIZE_MODE_FIT;
    }

    public static int getBackground() {
        return Math.clamp(Prefers.getInt("background", 2), MIN_BACKGROUND, MAX_BACKGROUND);
    }

    public static void putBackground(int background) {
        Prefers.put("background", Math.clamp(background, MIN_BACKGROUND, MAX_BACKGROUND));
    }

    public static boolean isBackgroundOff() {
        return getBackground() == 0;
    }

    public static boolean isBackgroundOn() {
        return getBackground() == 1 || getBackground() == 2;
    }

    public static boolean isBackgroundPiP() {
        return getBackground() == 2;
    }

    public static int getBuffer() {
        return Math.clamp(Prefers.getInt("buffer"), MIN_BUFFER, MAX_BUFFER);
    }

    public static void putBuffer(int buffer) {
        Prefers.put("buffer", Math.clamp(buffer, MIN_BUFFER, MAX_BUFFER));
    }
}
