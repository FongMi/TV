package com.fongmi.android.tv.setting;

import androidx.media3.common.DolbyVisionOutputPolicy;
import androidx.media3.common.DecoderMode;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

import com.github.catvod.utils.Prefers;

public class DecodeSetting {

    public static boolean isAudioPassThrough() {
        return Prefers.getBoolean("decode_audio_pass_through", Prefers.getBoolean("audio_pass_through", true));
    }

    public static void putAudioPassThrough(boolean audioPassThrough) {
        Prefers.put("decode_audio_pass_through", audioPassThrough);
    }

    public static String getDecoderModeText(DecoderMode mode) {
        return ResUtil.getString(switch (mode) {
            case AUTO -> R.string.decoder_mode_auto;
            case HARDWARE -> R.string.decoder_mode_hardware;
            case SOFTWARE -> R.string.decoder_mode_software;
            case FFMPEG -> R.string.decoder_mode_ffmpeg;
        });
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
