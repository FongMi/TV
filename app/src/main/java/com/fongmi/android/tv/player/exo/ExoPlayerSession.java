package com.fongmi.android.tv.player.exo;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.exoplayer.DecoderManager;
import androidx.media3.exoplayer.DecoderMode;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.libass.LibassConfiguration;
import androidx.media3.exoplayer.libass.LibassPlaybackSession;
import androidx.media3.exoplayer.libass.LibassSubtitleController;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager;
import androidx.media3.exoplayer.source.preload.PreloadException;
import androidx.media3.exoplayer.source.preload.PreloadManagerListener;
import androidx.media3.exoplayer.text.SecondaryTextOutput;
import androidx.media3.exoplayer.trackselection.SecondaryTextTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.subtitle.AndroidFontConfig;
import com.fongmi.android.tv.player.subtitle.ExternalFont;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.SubtitleSetting;

import java.io.File;

final class ExoPlayerSession {

    private static final String TAG = ExoPlayerSession.class.getSimpleName();
    private static final int MAX_PRELOAD_BUFFER_BYTES = 64 * 1024 * 1024;
    private static final int LIBASS_MAX_RENDER_PIXELS = 1920 * 1080;
    private static final int LIBASS_MAX_BITMAP_CACHE_SIZE_MB = 48;
    private static final int LIBASS_MAX_GLYPH_COUNT = 4096;
    private static final long PRELOAD_DURATION_MS = 10_000;

    private final DecoderManager decoderManager;
    private final ExoDecoderFallback decoderFallback;
    private final LibassSubtitleController libassSubtitleController;
    private final LibassPlaybackSession libassPlaybackSession;
    private final DefaultPreloadManager preloadManager;
    private final boolean libassEnabled;
    private final ExoPlayer player;
    private DecoderMode requestedAudioMode;
    private DecoderMode requestedVideoMode;

    @Nullable
    private PreloadRequest preloadRequest;

    ExoPlayerSession(Player.Listener listener, AudioProcessor audioProcessor) {
        this.requestedAudioMode = DecoderMode.AUTO;
        this.requestedVideoMode = DecoderMode.AUTO;
        this.decoderManager = new DecoderManager(requestedAudioMode, requestedVideoMode);
        TrackSelector.Factory decodeTrackSelectorFactory = context -> ExoUtil.buildTrackSelector(decoderManager);
        SecondaryTextTrackSelector.Factory secondaryTextTrackSelectorFactory = new SecondaryTextTrackSelector.Factory(decodeTrackSelectorFactory);
        SecondaryTextOutput secondaryTextOutput = new SecondaryTextOutput();
        this.libassEnabled = PlayerSetting.isLibass();
        this.libassPlaybackSession = createLibassPlaybackSession(libassEnabled);
        DefaultPreloadManager.Builder builder = createPreloadManagerBuilder(audioProcessor, secondaryTextTrackSelectorFactory, secondaryTextOutput);
        this.preloadManager = builder.build();
        this.preloadManager.addListener(new PreloadListener());
        this.player = ExoUtil.buildPlayer(listener, builder);
        this.decoderManager.attach(player);
        this.decoderFallback = new ExoDecoderFallback(decoderManager);
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

    @Nullable
    DecoderMode decoderMode(@C.TrackType int trackType) {
        return switch (trackType) {
            case C.TRACK_TYPE_AUDIO -> decoderManager.getAudioMode();
            case C.TRACK_TYPE_VIDEO -> decoderManager.getVideoMode();
            default -> null;
        };
    }

    void setDecoderMode(@C.TrackType int trackType, DecoderMode mode) {
        decoderFallback.reset(trackType);
        if (trackType != C.TRACK_TYPE_AUDIO && trackType != C.TRACK_TYPE_VIDEO) return;
        clearPreload();
        if (trackType == C.TRACK_TYPE_AUDIO) {
            requestedAudioMode = mode;
            decoderManager.selectAudioDecoder(mode);
        } else {
            requestedVideoMode = mode;
            decoderManager.selectVideoDecoder(mode);
        }
    }

    boolean switchDecoderForRetry(PlaybackException exception) {
        if (!(exception instanceof ExoPlaybackException error) || !decoderFallback.retry(error)) return false;
        clearPreload();
        return true;
    }

    void resetDecoderFallback(@C.TrackType int trackType) {
        decoderFallback.reset(trackType);
    }

    void prepareForNewMedia() {
        decoderFallback.reset(C.TRACK_TYPE_AUDIO);
        decoderFallback.reset(C.TRACK_TYPE_VIDEO);
        if (requestedAudioMode == decoderManager.getAudioMode() && requestedVideoMode == decoderManager.getVideoMode()) return;
        clearPreload();
        decoderManager.setDecoderModes(requestedAudioMode, requestedVideoMode);
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
        decoderManager.detach();
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
        return new DefaultPreloadManager.Builder(App.get(), ignored -> DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(getPreloadStartPositionMs(), PRELOAD_DURATION_MS)).setMediaSourceFactorySupplier(ExoMediaSourceFactory.supplier(libassPlaybackSession)).setRenderersFactory(ExoUtil.buildRenderersFactory(audioProcessor, secondaryTextOutput, libassPlaybackSession, decoderManager)).setTrackSelectorFactory(secondaryTextTrackSelectorFactory).setLoadControl(ExoUtil.buildLoadControl(MAX_PRELOAD_BUFFER_BYTES));
    }

    private static LibassPlaybackSession createLibassPlaybackSession(boolean libassEnabled) {
        File fontConfig = libassEnabled ? AndroidFontConfig.prepare() : null;
        String fontConfigPath = fontConfig != null && fontConfig.length() > 0 ? fontConfig.getAbsolutePath() : null;
        ExternalFont.Item font = libassEnabled ? SubtitleSetting.getFont() : null;
        String fontFamily = font == null ? null : font.familyName();
        String fontsDirectory = libassEnabled ? (font == null ? ExternalFont.getDirectory() : font.directory()).getAbsolutePath() : null;
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

}
