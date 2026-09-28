package com.fongmi.android.tv.setting;

import androidx.media3.common.DolbyVisionOutputPolicy;

import com.github.catvod.utils.Prefers;

public class DecodeSetting {

    public static boolean isAudioPassThrough() {
        return Prefers.getBoolean("decode_audio_pass_through", Prefers.getBoolean("audio_pass_through", true));
    }

    public static void putAudioPassThrough(boolean audioPassThrough) {
        Prefers.put("decode_audio_pass_through", audioPassThrough);
    }

    public static boolean isAudioPrefer() {
        return Prefers.getBoolean("decode_audio_prefer", Prefers.getBoolean("audio_prefer"));
    }

    public static void putAudioPrefer(boolean audioPrefer) {
        Prefers.put("decode_audio_prefer", audioPrefer);
    }

    public static boolean isVideoPrefer() {
        return Prefers.getBoolean("decode_video_prefer", Prefers.getBoolean("video_prefer"));
    }

    public static void putVideoPrefer(boolean videoPrefer) {
        Prefers.put("decode_video_prefer", videoPrefer);
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
