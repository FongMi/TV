package com.fongmi.android.tv.setting;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.DecoderMode;
import androidx.media3.common.DolbyVisionOutputPolicy;

import com.github.catvod.utils.Prefers;

public class DecodeSetting {

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
        if (engine == PlayerSetting.ENGINE_MPV) return trackType == C.TRACK_TYPE_VIDEO ? "decode_mpv_video_mode" : null;
        if (engine != PlayerSetting.ENGINE_EXO) return null;
        return switch (trackType) {
            case C.TRACK_TYPE_AUDIO -> "decode_exo_audio_mode";
            case C.TRACK_TYPE_VIDEO -> "decode_exo_video_mode";
            default -> null;
        };
    }

    private static boolean isDecoderModeSupported(int engine, DecoderMode mode) {
        return engine != PlayerSetting.ENGINE_MPV || mode != DecoderMode.SOFTWARE;
    }

    public static boolean isAudioPassThrough() {
        return Prefers.getBoolean("decode_audio_pass_through", Prefers.getBoolean("audio_pass_through", true));
    }

    public static void putAudioPassThrough(boolean audioPassThrough) {
        Prefers.put("decode_audio_pass_through", audioPassThrough);
    }

    public static @DolbyVisionOutputPolicy.Mode int getDolbyVisionOutputPolicy() {
        int mode = Prefers.getInt("decode_dolby_vision_output_policy", DolbyVisionOutputPolicy.AUTO);
        return mode >= DolbyVisionOutputPolicy.AUTO && mode <= DolbyVisionOutputPolicy.ASSUME_UNSUPPORTED ? mode : DolbyVisionOutputPolicy.AUTO;
    }

    public static void putDolbyVisionOutputPolicy(@DolbyVisionOutputPolicy.Mode int mode) {
        Prefers.put("decode_dolby_vision_output_policy", mode);
    }

    public static boolean isPreferAAC() {
        return Prefers.getBoolean("decode_prefer_aac", Prefers.getBoolean("prefer_aac"));
    }

    public static void putPreferAAC(boolean preferAAC) {
        Prefers.put("decode_prefer_aac", preferAAC);
    }

    public static boolean isTunnel() {
        return Prefers.getBoolean("decode_tunnel", Prefers.getBoolean("tunnel"));
    }

    public static void putTunnel(boolean tunnel) {
        Prefers.put("decode_tunnel", tunnel);
        if (PlayerSetting.isExo() && tunnel) PlayerSetting.putRender(PlayerSetting.RENDER_SURFACE);
    }

    public static boolean isTunnelingEnabled() {
        return isTunnel() && PlayerSetting.getRender() == PlayerSetting.RENDER_SURFACE;
    }
}
