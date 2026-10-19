package com.fongmi.android.tv.player.mpv;

import android.content.pm.PackageManager;

import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.ProgressiveIsoCache;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.mpvplayer.MpvAndroidOptions;
import androidx.media3.mpvplayer.MpvDecoderMode;
import androidx.media3.mpvplayer.MpvPlayer;
import androidx.media3.mpvplayer.MpvPlayerConfig;
import androidx.media3.mpvplayer.MpvSubtitleOptions;
import androidx.media3.ui.CaptionStyleCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.player.exo.ExoMediaSourceFactory;
import com.fongmi.android.tv.player.subtitle.AndroidFontConfig;
import com.fongmi.android.tv.player.subtitle.ExternalFont;
import com.fongmi.android.tv.player.subtitle.SubtitleFileContent;
import com.fongmi.android.tv.player.track.LangUtil;
import com.fongmi.android.tv.setting.DecodeSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.PreloadSetting;
import com.fongmi.android.tv.setting.SubtitleSetting;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.Proxy;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.RuntimeModule;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class MpvUtil {

    private static final List<String> FONT_OPTIONS = List.of("sub-font", "sub-fonts-dir", "sub-ass-style-overrides");
    private static final List<String> SCALE_OPTIONS = List.of("sub-scale", "sub-scale-signs", "secondary-sub-scale");
    private static final List<String> CACHE_OPTIONS = List.of("cache", "cache-on-disk", "demuxer-cache-dir", "cache-secs");
    private static final List<String> PLAYER_OPTIONS = List.of("vo", "gpu-api", "gpu-context", "force-window", "audio-spdif", "android-dolby-vision-output", "demuxer-dovi-profile7");
    private static final List<String> STYLE_OPTIONS = List.of("sub-color", "sub-back-color", "sub-border-style", "sub-outline-color", "sub-outline-size", "sub-shadow-offset");
    private static final List<String> IMAGE_SUBTITLE_OPTIONS = List.of("image-subs-brightness", "image-subs-override", "image-subs-color");
    private static final List<String> PLAYBACK_OPTIONS = List.of("pause", "loop-file", "speed", "pitch", "audio-delay", "sub-delay", "secondary-sub-delay", "panscan", "video-scale-x", "video-scale-y", "slang");
    private static final List<String> VIDEO_EFFECT_OPTIONS = List.of("brightness", "contrast", "saturation", "gamma", "hue", "sharpen");

    private static final String ASSET_CA_FILE = "cacert.pem";
    private static final int VULKAN_1_2 = 0x00402000;
    private static String runtimeError;

    public static boolean checkInstalled() {
        if (RuntimeModule.MPV.isInstalled()) return true;
        Notify.show(ResUtil.getString(R.string.error_module_missing, RuntimeModule.MPV.title));
        return false;
    }

    public static boolean isAvailable() {
        try {
            boolean available = MpvPlayer.isAvailable();
            if (available) runtimeError = null;
            return available;
        } catch (RuntimeException | LinkageError e) {
            String error = Notify.getError(e);
            if (error == null) error = ResUtil.getString(R.string.error_mpv_runtime, Notify.getError(R.string.module_failed_title, e));
            if (!error.equals(runtimeError)) Notify.show(error);
            runtimeError = error;
            return false;
        }
    }

    public static boolean loadRuntime() {
        synchronized (RuntimeModule.MPV) {
            try {
                if (RuntimeModule.MPV.isRestartRequired()) throw new IllegalStateException(ResUtil.getString(R.string.module_restart));
                RuntimeModule.MPV.load();
                return true;
            } catch (IOException e) {
                throw new IllegalStateException("Unable to prepare MPV module", e);
            }
        }
    }

    public static boolean isVulkanSupported() {
        return App.get().getPackageManager().hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, VULKAN_1_2);
    }

    public static MpvPlayer buildPlayer(Player.Listener listener) {
        MpvPlayer player = new MpvPlayer.Builder(App.get()).setVideoDecoderMode(MpvDecoderMode.AUTO).setConfig(buildConfig()).build();
        player.setSubtitleContentLoader(new SubtitleFileContent());
        setPreferredTextLanguages(player);
        player.addListener(listener);
        return player;
    }

    public static void applySubtitleStyle(MpvPlayer player) {
        player.setSubtitleOptions(buildSubtitleOptions(SubtitleSetting.getFont()));
    }

    static List<String> getManagedOptionNames() {
        List<String> options = new ArrayList<>(PLAYER_OPTIONS);
        options.add("hwdec-software-fallback");
        options.addAll(PLAYBACK_OPTIONS);
        options.addAll(VIDEO_EFFECT_OPTIONS);
        options.addAll(List.of("sub-pos", "secondary-sub-pos", "sub-offset-y", "secondary-sub-offset-y", "sub-ass-override", "secondary-sub-ass-override", "embeddedfonts"));
        options.addAll(SCALE_OPTIONS);
        boolean hasFontOverride = SubtitleSetting.getFont() != null;
        if (PreloadSetting.isEnabled()) options.addAll(CACHE_OPTIONS);
        if (hasFontOverride) options.addAll(FONT_OPTIONS);
        options.addAll(STYLE_OPTIONS);
        options.addAll(IMAGE_SUBTITLE_OPTIONS);
        if (SubtitleSetting.getSecondaryMode() != SubtitleSetting.SECONDARY_MODE_DEFAULT) options.add("secondary-sid");
        return options;
    }

    private static DataSource.Factory createDataSourceFactory() {
        Cache cache = ExoMediaSourceFactory.getCache();
        CacheDataSource.Factory factory = new CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(new OkHttpDataSource.Factory(OkHttp.player())).setCacheWriteDataSinkFactory(null).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        return ProgressiveIsoCache.withDiskCache(factory, cache);
    }

    static MpvPlayerConfig buildConfig() {
        ExternalFont.Item font = SubtitleSetting.getFont();
        File cacheDir = Path.mpvCache();
        File defaultFontsDirectory = ExternalFont.getDirectory();
        AndroidFontConfig.prepare();
        MpvPlayerConfig.Builder builder = new MpvPlayerConfig.Builder().setDataSourceFactory(createDataSourceFactory()).setDefaultFontDirectories(defaultFontsDirectory, defaultFontsDirectory).addConfigDirectory(Path.mpv()).addAndroidDefaults(buildAndroidOptions(cacheDir));
        builder.addPreInitStringOption("proxy-url", "http://127.0.0.1:" + Proxy.getPort() + "/proxy?");
        builder.addTlsCaFileFromAsset(App.get(), ASSET_CA_FILE, Path.files(ASSET_CA_FILE)).addAndroidSubtitleOptions(App.get(), buildSubtitleOptions(font));
        if (PreloadSetting.isEnabled()) builder.addDiskCacheOptions(cacheDir, PreloadSetting.getTimeSeconds());
        return builder.build();
    }

    public static List<MpvScripts.Item> readScripts() {
        try {
            return MpvScripts.read();
        } catch (JSONException e) {
            Notify.show(Notify.getError(R.string.mpv_script_error, e));
            return List.of();
        }
    }

    private static MpvAndroidOptions buildAndroidOptions(File shaderCacheDirectory) {
        MpvAndroidOptions.Builder builder = new MpvAndroidOptions.Builder().setShaderCacheDirectory(shaderCacheDirectory).setAudioPassthroughEnabled(DecodeSetting.isAudioPassThrough()).setDolbyVisionOutputPolicy(DecodeSetting.getDolbyVisionOutputPolicy());
        builder.setVulkanEnabled(isVulkanSupported() && PlayerSetting.isMpvVulkan());
        builder.setGpuNextEnabled(PlayerSetting.isMpvGpuNext());
        return builder.build();
    }

    static void setPreferredTextLanguages(MpvPlayer player) {
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setPreferredTextLanguages(LangUtil.getPreferredTextLanguages()).build());
    }

    static MpvSubtitleOptions buildSubtitleOptions(@Nullable ExternalFont.Item font) {
        MpvSubtitleOptions.Builder builder = new MpvSubtitleOptions.Builder();
        if (font != null) builder.setFontsDirectory(font.directory().getAbsolutePath());
        builder.setPosition(100.0);
        builder.setVerticalOffset(SubtitleSetting.getPosition());
        builder.setScale(SubtitleSetting.getAppliedScale());
        builder.setBitmapSubtitleBrightness(SubtitleSetting.getBitmapBrightness());
        builder.setSecondarySubtitlePosition(100.0);
        builder.setSecondaryVerticalOffset(100.0 - SubtitleSetting.getSecondaryPosition());
        builder.setSecondaryAssStyleOverride(SubtitleSetting.isStyleForced());
        if (font != null) builder.setFontFamily(font.familyName());
        setCaptionStyle(builder);
        return builder.build();
    }

    private static void setCaptionStyle(MpvSubtitleOptions.Builder builder) {
        if (SubtitleSetting.isCustomStyle()) {
            builder.setCustomStyle(
                    SubtitleSetting.getTextColor(),
                    SubtitleSetting.getBackgroundColor(),
                    SubtitleSetting.getEdgeType(),
                    SubtitleSetting.getEdgeColor(),
                    SubtitleSetting.getEdgeWidth(),
                    SubtitleSetting.getShadow());
        } else if (SubtitleSetting.isSystemStyle()) {
            builder.setSystemCaptionStyle();
        } else {
            CaptionStyleCompat style = SubtitleSetting.getStyle();
            builder.setDefaultStyle(style.foregroundColor, style.backgroundColor, style.edgeType, style.edgeColor, style.edgeWidth, style.shadowOffset);
        }
    }
}
