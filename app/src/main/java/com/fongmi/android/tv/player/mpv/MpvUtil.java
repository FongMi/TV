package com.fongmi.android.tv.player.mpv;

import androidx.media3.common.Player;
import androidx.media3.common.util.Util;
import androidx.media3.mpvplayer.MpvAndroidOptions;
import androidx.media3.mpvplayer.MpvPlayer;
import androidx.media3.mpvplayer.MpvPlayerConfig;
import androidx.media3.mpvplayer.MpvSubtitleOptions;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.track.LangUtil;
import com.fongmi.android.tv.setting.DecodeSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.PreloadSetting;
import com.fongmi.android.tv.setting.SubtitleSetting;
import com.github.catvod.utils.Path;

import java.io.File;

public final class MpvUtil {

    private static final String ASSET_CA_FILE = "cacert.pem";
    private static final double DEFAULT_SUB_POS = 100.0;
    private static final double DEFAULT_SUB_SCALE = 1.0;
    private static final double MIN_SUB_POS = 0.0;
    private static final double MAX_SUB_POS = 150.0;

    public static boolean isAvailable() {
        try {
            return MpvPlayer.isAvailable();
        } catch (Throwable e) {
            return false;
        }
    }

    public static MpvPlayer buildPlayer(int decode, Player.Listener listener) {
        MpvPlayer player = new MpvPlayer.Builder(App.get()).setDecode(decode).setConfig(buildConfig()).build();
        setPreferredTextLanguages(player);
        player.addListener(listener);
        return player;
    }

    public static void setSubtitleStyle(MpvPlayer player) {
        player.setSubtitleOptions(buildSubtitleOptions());
    }

    private static MpvPlayerConfig buildConfig() {
        File configDir = Path.mpv();
        File cacheDir = Path.mpvCache();
        MpvPlayerConfig.Builder builder = new MpvPlayerConfig.Builder().addConfigDirectory(configDir).addAndroidFontConfig(configDir, cacheDir).addAndroidDefaults(buildAndroidOptions(cacheDir)).addTlsCaFileFromAsset(App.get(), ASSET_CA_FILE, Path.files(ASSET_CA_FILE)).addAndroidSubtitleOptions(App.get(), buildSubtitleOptions());
        addPreloadOptions(builder);
        return builder.build();
    }

    private static MpvAndroidOptions buildAndroidOptions(File shaderCacheDirectory) {
        MpvAndroidOptions.Builder builder = new MpvAndroidOptions.Builder().setShaderCacheDirectory(shaderCacheDirectory).setAudioPassthroughEnabled(DecodeSetting.isAudioPassThrough()).setDolbyVisionOutputPolicy(DecodeSetting.getDolbyVisionOutputPolicy());
        builder.setGpuNextEnabled(PlayerSetting.isMpvGpuNext());
        builder.setVulkanEnabled(PlayerSetting.isMpvVulkan());
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
        MpvSubtitleOptions.Builder builder = new MpvSubtitleOptions.Builder().setPosition(getSubtitlePosition()).setScale(getSubtitleScale()).setSecondarySubtitle(SubtitleSetting.getSecondaryTrackId(), SubtitleSetting.getSecondaryPosition(), SubtitleSetting.isStyleForced());
        if (SubtitleSetting.isCustomStyle()) builder.setCustomStyle(SubtitleSetting.getTextColor(), SubtitleSetting.getBackgroundColor(), SubtitleSetting.getEdgeType(), SubtitleSetting.getEdgeColor(), SubtitleSetting.getEdgeWidth(), SubtitleSetting.getShadow());
        else if (SubtitleSetting.isSystemStyle()) builder.setSystemCaptionStyle();
        return builder.build();
    }

    private static double getSubtitlePosition() {
        float position = SubtitleSetting.getPosition();
        if (!SubtitleSetting.isPositionSet()) return DEFAULT_SUB_POS;
        return Util.constrainValue(DEFAULT_SUB_POS - position, MIN_SUB_POS, MAX_SUB_POS);
    }

    private static double getSubtitleScale() {
        if (!SubtitleSetting.isScaleApplied()) return DEFAULT_SUB_SCALE;
        return SubtitleSetting.getScale(App.get());
    }
}
