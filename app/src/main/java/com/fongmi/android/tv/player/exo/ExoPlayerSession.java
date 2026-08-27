package com.fongmi.android.tv.player.exo;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.libass.LibassConfiguration;
import androidx.media3.exoplayer.libass.LibassPlaybackSession;
import androidx.media3.exoplayer.libass.LibassSubtitleController;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager;
import androidx.media3.exoplayer.source.preload.PreloadException;
import androidx.media3.exoplayer.source.preload.PreloadManagerListener;
import androidx.media3.exoplayer.text.SecondaryTextOutput;
import androidx.media3.exoplayer.trackselection.DecodeTrackSelector;
import androidx.media3.exoplayer.trackselection.SecondaryTextTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.subtitle.AndroidFontConfig;
import com.fongmi.android.tv.player.subtitle.ExternalFont;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.SubtitleSetting;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class ExoPlayerSession {

    private static final String TAG = ExoPlayerSession.class.getSimpleName();
    private static final int MAX_PRELOAD_BUFFER_BYTES = 64 * 1024 * 1024;
    private static final int LIBASS_MAX_RENDER_PIXELS = 1920 * 1080;
    private static final int LIBASS_MAX_BITMAP_CACHE_SIZE_MB = 48;
    private static final int LIBASS_MAX_GLYPH_COUNT = 4096;
    private static final long PRELOAD_DURATION_MS = 10_000;

    private final DecodeTrackSelectorFactory decodeTrackSelectorFactory;
    private final LibassSubtitleController libassSubtitleController;
    private final LibassPlaybackSession libassPlaybackSession;
    private final DefaultPreloadManager preloadManager;
    private final boolean libassEnabled;
    private final ExoPlayer player;

    @Nullable
    private PreloadRequest preloadRequest;

    ExoPlayerSession(int decode, Player.Listener listener, AudioProcessor audioProcessor) {
        this.decodeTrackSelectorFactory = new DecodeTrackSelectorFactory(decode);
        SecondaryTextTrackSelector.Factory secondaryTextTrackSelectorFactory = new SecondaryTextTrackSelector.Factory(decodeTrackSelectorFactory);
        SecondaryTextOutput secondaryTextOutput = new SecondaryTextOutput();
        this.libassEnabled = PlayerSetting.isLibass();
        this.libassPlaybackSession = createLibassPlaybackSession(libassEnabled);
        DefaultPreloadManager.Builder builder = createPreloadManagerBuilder(audioProcessor, secondaryTextTrackSelectorFactory, secondaryTextOutput);
        this.preloadManager = builder.build();
        this.preloadManager.addListener(new PreloadListener());
        this.player = ExoUtil.buildPlayer(listener, builder);
        this.libassSubtitleController = new LibassSubtitleController(player, libassPlaybackSession, secondaryTextTrackSelectorFactory, secondaryTextOutput);
    }

    ExoPlayer player() {
        return player;
    }

    LibassPlaybackSession libassPlaybackSession() {
        return libassPlaybackSession;
    }

    LibassSubtitleController libassSubtitleController() {
        return libassSubtitleController;
    }

    boolean hasLibassSettingChanged() {
        return libassEnabled != PlayerSetting.isLibass();
    }

    void setDecode(int decode) {
        decodeTrackSelectorFactory.setDecode(decode);
    }

    void preload(MediaItem mediaItem, long startPositionMs) {
        PreloadRequest request = new PreloadRequest(mediaItem, Math.max(0, startPositionMs));
        if (request.equals(preloadRequest)) return;
        clearPreload();
        preloadRequest = request;
        libassPlaybackSession.setPreloadMediaItem(mediaItem);
        preloadManager.add(request.mediaItem(), 0);
        preloadManager.invalidate();
    }

    @Nullable
    MediaSource usePreloadedMediaSource(MediaItem mediaItem) {
        PreloadRequest request = preloadRequest;
        if (request != null && mediaItem.equals(request.mediaItem())) return preloadManager.getMediaSource(request.mediaItem());
        clearPreload();
        return null;
    }

    void clearPreload() {
        PreloadRequest request = preloadRequest;
        libassPlaybackSession.setPreloadMediaItem(null);
        if (request == null) return;
        preloadRequest = null;
        preloadManager.remove(request.mediaItem());
    }

    void release() {
        preloadRequest = null;
        libassSubtitleController.close();
        preloadManager.release();
        player.release();
        libassPlaybackSession.close();
    }

    private boolean isPreloaded(MediaItem mediaItem) {
        return preloadRequest != null && mediaItem.equals(preloadRequest.mediaItem());
    }

    private long getPreloadStartPositionMs() {
        return preloadRequest == null ? 0 : preloadRequest.startPositionMs();
    }

    private DefaultPreloadManager.Builder createPreloadManagerBuilder(AudioProcessor audioProcessor, SecondaryTextTrackSelector.Factory secondaryTextTrackSelectorFactory, SecondaryTextOutput secondaryTextOutput) {
        return new DefaultPreloadManager.Builder(App.get(), ignored -> DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(getPreloadStartPositionMs(), PRELOAD_DURATION_MS)).setMediaSourceFactorySupplier(ExoMediaSourceFactory.supplier(libassPlaybackSession)).setRenderersFactory(ExoUtil.buildRenderersFactory(audioProcessor, secondaryTextOutput, libassPlaybackSession)).setTrackSelectorFactory(secondaryTextTrackSelectorFactory).setLoadControl(ExoUtil.buildLoadControl(MAX_PRELOAD_BUFFER_BYTES));
    }

    private static LibassPlaybackSession createLibassPlaybackSession(boolean libassEnabled) {
        File fontConfig = libassEnabled ? AndroidFontConfig.prepare() : null;
        String fontConfigPath = fontConfig != null && fontConfig.length() > 0 ? fontConfig.getAbsolutePath() : null;
        String fontFamily = libassEnabled ? SubtitleSetting.getFontFamily() : null;
        String fontsDirectory = libassEnabled ? ExternalFont.getDirectory().getAbsolutePath() : null;
        LibassConfiguration configuration = new LibassConfiguration.Builder().setFontConfig(fontConfigPath).setFontsDirectory(fontsDirectory).setDefaultFontFamily(fontFamily).setMaximumRenderPixels(LIBASS_MAX_RENDER_PIXELS).setMaximumGlyphCount(LIBASS_MAX_GLYPH_COUNT).setMaximumBitmapCacheSizeMb(LIBASS_MAX_BITMAP_CACHE_SIZE_MB).build();
        return new LibassPlaybackSession(configuration, libassEnabled);
    }

    private final class PreloadListener implements PreloadManagerListener {

        @Override
        public void onCompleted(@NonNull MediaItem mediaItem) {
            if (isPreloaded(mediaItem)) Log.d(TAG, "Preload completed");
        }

        @Override
        public void onError(PreloadException exception) {
            if (!isPreloaded(exception.mediaItem)) return;
            Log.w(TAG, "Preload failed", exception);
            clearPreload();
        }
    }

    private record PreloadRequest(MediaItem mediaItem, long startPositionMs) {
    }

    private static final class DecodeTrackSelectorFactory implements TrackSelector.Factory {

        private final List<DecodeTrackSelector> trackSelectors = new ArrayList<>(2);
        private int decode;

        private DecodeTrackSelectorFactory(int decode) {
            this.decode = decode;
        }

        @NonNull
        @Override
        public TrackSelector createTrackSelector(@NonNull Context context) {
            DecodeTrackSelector trackSelector = ExoUtil.buildTrackSelector(decode);
            trackSelectors.add(trackSelector);
            return trackSelector;
        }

        private void setDecode(int decode) {
            if (this.decode == decode) return;
            this.decode = decode;
            for (DecodeTrackSelector trackSelector : trackSelectors) ExoUtil.setDecodePreferences(trackSelector, decode);
        }
    }
}
