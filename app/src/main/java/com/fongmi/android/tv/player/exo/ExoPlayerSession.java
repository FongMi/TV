package com.fongmi.android.tv.player.exo;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.preload.DefaultPreloadManager;
import androidx.media3.exoplayer.source.preload.PreloadException;
import androidx.media3.exoplayer.source.preload.PreloadManagerListener;
import androidx.media3.exoplayer.trackselection.DecodeTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;

import com.fongmi.android.tv.App;

import java.util.ArrayList;
import java.util.List;

final class ExoPlayerSession {

    private static final String TAG = ExoPlayerSession.class.getSimpleName();
    private static final int MAX_PRELOAD_BUFFER_BYTES = 64 * 1024 * 1024;
    private static final long PRELOAD_DURATION_MS = 10_000;

    private final DecodeTrackSelectorFactory trackSelectorFactory;
    private final DefaultPreloadManager preloadManager;
    private final ExoPlayer player;

    @Nullable
    private PreloadRequest preloadRequest;

    ExoPlayerSession(int decode, Player.Listener listener, AudioProcessor audioProcessor) {
        this.trackSelectorFactory = new DecodeTrackSelectorFactory(decode);
        DefaultPreloadManager.Builder builder = new DefaultPreloadManager.Builder(App.get(), ignored -> DefaultPreloadManager.PreloadStatus.specifiedRangeLoaded(getPreloadStartPositionMs(), PRELOAD_DURATION_MS)).setMediaSourceFactorySupplier(ExoMediaSourceFactory.supplier()).setRenderersFactory(ExoUtil.buildRenderersFactory(audioProcessor)).setTrackSelectorFactory(trackSelectorFactory).setLoadControl(ExoUtil.buildLoadControl(MAX_PRELOAD_BUFFER_BYTES));
        this.preloadManager = builder.build();
        this.preloadManager.addListener(new PreloadListener());
        this.player = ExoUtil.buildPlayer(listener, builder);
    }

    ExoPlayer player() {
        return player;
    }

    void setDecode(int decode) {
        trackSelectorFactory.setDecode(decode);
    }

    void preload(MediaItem mediaItem, long startPositionMs) {
        PreloadRequest request = new PreloadRequest(mediaItem, Math.max(0, startPositionMs));
        if (request.equals(preloadRequest)) return;
        clearPreload();
        preloadRequest = request;
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
        if (request == null) return;
        preloadRequest = null;
        preloadManager.remove(request.mediaItem());
    }

    void release() {
        preloadRequest = null;
        preloadManager.release();
        player.release();
    }

    private boolean isPreloaded(MediaItem mediaItem) {
        return preloadRequest != null && mediaItem.equals(preloadRequest.mediaItem());
    }

    private long getPreloadStartPositionMs() {
        return preloadRequest == null ? 0 : preloadRequest.startPositionMs();
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
