package com.fongmi.android.tv.setting;

import androidx.media3.common.DolbyVisionOutputPolicy;

import com.github.catvod.utils.Prefers;

public class OutputSetting {

    public static final int RENDER_SURFACE = 0;
    public static final int RENDER_TEXTURE = 1;

    public static int getRender() {
        return Math.clamp(Prefers.getInt("output_render", RENDER_SURFACE), RENDER_SURFACE, RENDER_TEXTURE);
    }

    public static void putRender(int render) {
        Prefers.put("output_render", Math.clamp(render, RENDER_SURFACE, RENDER_TEXTURE));
        if (PlayerSetting.isExo() && isTunnel() && getRender() == RENDER_TEXTURE) putTunnel(false);
    }

    public static @DolbyVisionOutputPolicy.Mode int getDolbyVisionOutputPolicy() {
        int mode = Prefers.getInt("output_dolby_vision_policy", DolbyVisionOutputPolicy.AUTO);
        return mode >= DolbyVisionOutputPolicy.AUTO && mode <= DolbyVisionOutputPolicy.ASSUME_UNSUPPORTED ? mode : DolbyVisionOutputPolicy.AUTO;
    }

    public static void putDolbyVisionOutputPolicy(@DolbyVisionOutputPolicy.Mode int mode) {
        Prefers.put("output_dolby_vision_policy", mode);
    }

    public static boolean isAudioPassThrough() {
        return Prefers.getBoolean("output_audio_pass_through", true);
    }

    public static void putAudioPassThrough(boolean audioPassThrough) {
        Prefers.put("output_audio_pass_through", audioPassThrough);
    }

    public static boolean isTunnel() {
        return Prefers.getBoolean("output_tunnel");
    }

    public static void putTunnel(boolean tunnel) {
        Prefers.put("output_tunnel", tunnel);
        if (tunnel) putRender(RENDER_SURFACE);
    }

    public static boolean isTunnelingEnabled() {
        return isTunnel() && getRender() == RENDER_SURFACE;
    }

    public static int getMotion() {
        return Math.clamp(Prefers.getInt("output_motion"), 0, 2);
    }

    public static void putMotion(int motion) {
        Prefers.put("output_motion", Math.clamp(motion, 0, 2));
    }

    public static int getMotionFps() {
        return getMotionFps(getMotion());
    }

    public static int getMotionFps(int motion) {
        return switch (motion) {
            case 1 -> 60;
            case 2 -> 120;
            default -> 0;
        };
    }

    public static boolean isMpvGpuNext() {
        return Prefers.getBoolean("output_mpv_gpu_next");
    }

    public static void putMpvGpuNext(boolean gpuNext) {
        Prefers.put("output_mpv_gpu_next", gpuNext);
    }

    public static boolean isMpvVulkan() {
        return Prefers.getBoolean("output_mpv_vulkan");
    }

    public static void putMpvVulkan(boolean vulkan) {
        Prefers.put("output_mpv_vulkan", vulkan);
    }

    public static boolean isPreferAAC() {
        return Prefers.getBoolean("output_prefer_aac");
    }

    public static void putPreferAAC(boolean preferAAC) {
        Prefers.put("output_prefer_aac", preferAAC);
    }
}
