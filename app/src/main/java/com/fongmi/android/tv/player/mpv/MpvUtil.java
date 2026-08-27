package com.fongmi.android.tv.player.mpv;

import android.content.pm.PackageManager;

import androidx.media3.common.Player;
import androidx.media3.common.util.Util;
import androidx.media3.mpvplayer.MpvAndroidOptions;
import androidx.media3.mpvplayer.MpvPlayer;
import androidx.media3.mpvplayer.MpvPlayerConfig;
import androidx.media3.mpvplayer.MpvSubtitleOptions;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.subtitle.AndroidFontConfig;
import com.fongmi.android.tv.player.subtitle.ExternalFont;
import com.fongmi.android.tv.player.track.LangUtil;
import com.fongmi.android.tv.setting.DecodeSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.PreloadSetting;
import com.fongmi.android.tv.setting.SubtitleSetting;
import com.github.catvod.utils.Path;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class MpvUtil {

    private static final List<String> FONT_OPTIONS = List.of("sub-font", "sub-fonts-dir", "sub-ass-style-overrides");
    private static final List<String> SCALE_OPTIONS = List.of("sub-scale", "sub-scale-signs");
    private static final List<String> CACHE_OPTIONS = List.of("cache", "cache-on-disk", "demuxer-cache-dir", "cache-secs");
    private static final List<String> PLAYER_OPTIONS = List.of("vo", "gpu-api", "gpu-context", "hwdec", "audio-spdif", "android-dolby-vision-output", "demuxer-dovi-profile7");
    private static final List<String> STYLE_OPTIONS = List.of("embeddedfonts", "sub-color", "sub-back-color", "sub-border-style", "sub-outline-color", "sub-outline-size", "sub-shadow-offset", "secondary-sub-ass-override");

    private static final String ASSET_CA_FILE = "cacert.pem";
    private static final int VULKAN_1_2 = 0x00402000;
    private static final double DEFAULT_SUB_POS = 100.0;
    private static final double MIN_SUB_POS = 0.0;
    private static final double MAX_SUB_POS = 150.0;

    public static boolean isAvailable() {
        try {
            return MpvPlayer.isAvailable();
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean isVulkanSupported() {
        return App.get().getPackageManager().hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, VULKAN_1_2);
    }

    public static MpvPlayer buildPlayer(int decode, Player.Listener listener) {
        MpvPlayer player = new MpvPlayer.Builder(App.get()).setDecode(decode).setConfig(buildConfig()).build();
        setPreferredTextLanguages(player);
        player.addListener(listener);
        return player;
    }

    public static void applySubtitleStyle(MpvPlayer player) {
        player.setSubtitleOptions(buildSubtitleOptions());
    }

    static List<String> getManagedOptionNames() {
        List<String> options = new ArrayList<>(PLAYER_OPTIONS);
        boolean hasFontOverride = SubtitleSetting.getFont() != null;
        boolean hasStyleOverride = SubtitleSetting.isStyleForced();
        if (PreloadSetting.isEnabled()) options.addAll(CACHE_OPTIONS);
        if (hasFontOverride) options.addAll(FONT_OPTIONS);
        if (hasStyleOverride) options.addAll(STYLE_OPTIONS);
        if (hasFontOverride || hasStyleOverride) options.add("sub-ass-override");
        if (SubtitleSetting.isPositionSet()) options.add("sub-pos");
        if (SubtitleSetting.isScaleApplied()) options.addAll(SCALE_OPTIONS);
        if (SubtitleSetting.isSecondaryPositionSet()) options.add("secondary-sub-pos");
        if (SubtitleSetting.getSecondaryMode() != SubtitleSetting.SECONDARY_MODE_DEFAULT) options.add("secondary-sid");
        return options;
    }

    private static MpvPlayerConfig buildConfig() {
        File cacheDir = Path.mpvCache();
        AndroidFontConfig.prepare();
        MpvPlayerConfig.Builder builder = new MpvPlayerConfig.Builder().addConfigDirectory(Path.mpv()).addAndroidDefaults(buildAndroidOptions(cacheDir)).addTlsCaFileFromAsset(App.get(), ASSET_CA_FILE, Path.files(ASSET_CA_FILE)).addAndroidSubtitleOptions(App.get(), buildSubtitleOptions());
        addPreloadOptions(builder);
        return builder.build();
    }

    private static MpvAndroidOptions buildAndroidOptions(File shaderCacheDirectory) {
        MpvAndroidOptions.Builder builder = new MpvAndroidOptions.Builder().setShaderCacheDirectory(shaderCacheDirectory).setAudioPassthroughEnabled(DecodeSetting.isAudioPassThrough()).setDolbyVisionOutputPolicy(DecodeSetting.getDolbyVisionOutputPolicy());
        builder.setVulkanEnabled(isVulkanSupported() && PlayerSetting.isMpvVulkan());
        builder.setGpuNextEnabled(PlayerSetting.isMpvGpuNext());
        return builder.build();
    }

    private static void setPreferredTextLanguages(MpvPlayer player) {
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setPreferredTextLanguages(LangUtil.getPreferredTextLanguages()).build());
    }

    private static void addPreloadOptions(MpvPlayerConfig.Builder builder) {
        if (!PreloadSetting.isEnabled()) return;
        builder.addDiskCacheOptions(Path.mpvCache(), PreloadSetting.getTimeSeconds());
    }

    private static MpvSubtitleOptions buildSubtitleOptions() {
        MpvSubtitleOptions.Builder builder = new MpvSubtitleOptions.Builder();
        if (SubtitleSetting.isPositionSet()) builder.setPosition(getSubtitlePosition());
        if (SubtitleSetting.isScaleApplied()) builder.setScale(SubtitleSetting.getAppliedScale());
        if (SubtitleSetting.isSecondaryPositionSet()) builder.setSecondarySubtitlePosition(SubtitleSetting.getSecondaryPosition());
        if (SubtitleSetting.isStyleForced()) builder.setSecondaryAssStyleOverride(true);
        String fontFamily = SubtitleSetting.getFontFamily();
        if (fontFamily != null) builder.setFontFamily(fontFamily).setFontsDirectory(ExternalFont.getDirectory().getAbsolutePath());
        if (SubtitleSetting.isCustomStyle()) builder.setCustomStyle(SubtitleSetting.getTextColor(), SubtitleSetting.getBackgroundColor(), SubtitleSetting.getEdgeType(), SubtitleSetting.getEdgeColor(), SubtitleSetting.getEdgeWidth(), SubtitleSetting.getShadow());
        else if (SubtitleSetting.isSystemStyle()) builder.setSystemCaptionStyle();
        return builder.build();
    }

    private static double getSubtitlePosition() {
        return Util.constrainValue(DEFAULT_SUB_POS - SubtitleSetting.getPosition(), MIN_SUB_POS, MAX_SUB_POS);
    }
}
