package com.fongmi.android.tv.playback.vod;

import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.playback.PlaybackResult;
import com.fongmi.android.tv.player.extractor.Source;

import java.util.function.LongSupplier;

final class VodPreloader {

    private static final long PRELOAD_DELAY_MS = 15_000;

    private final VodPlaybackHost host;
    private final VodDataSource dataSource;
    private final VodPlaybackState state;
    private final LongSupplier clock;
    private long playedMs;
    private long playingSinceMs = C.TIME_UNSET;
    private boolean waiting;

    VodPreloader(VodPlaybackHost host, VodDataSource dataSource, VodPlaybackState state) {
        this(host, dataSource, state, SystemClock::elapsedRealtime);
    }

    VodPreloader(VodPlaybackHost host, VodDataSource dataSource, VodPlaybackState state, LongSupplier clock) {
        this.host = host;
        this.dataSource = dataSource;
        this.state = state;
        this.clock = clock;
    }

    void update(Result result) {
        clear();
        if (result.needParse() || state.isUseParse() || findNextEpisode() == null) return;
        waiting = true;
        onPlayingChanged(host.isPlayerPlaying());
    }

    void onPlayingChanged(boolean playing) {
        if (!waiting) return;
        long now = clock.getAsLong();
        if (playingSinceMs != C.TIME_UNSET) playedMs += now - playingSinceMs;
        playingSinceMs = playing ? now : C.TIME_UNSET;
    }

    void onTimeChanged() {
        if (!waiting) return;
        onPlayingChanged(host.isPlayerPlaying());
        if (playingSinceMs != C.TIME_UNSET && playedMs >= PRELOAD_DELAY_MS) preloadNext();
    }

    private void preloadNext() {
        Episode episode = findNextEpisode();
        clear();
        if (episode == null) return;
        VodPlayRequest request = VodPlayRequest.create(host.getVodKey(), state.getFlag(), episode, state.isBuiltin(), state.getQualityPosition(), C.TIME_UNSET);
        state.beginPreload(request);
        dataSource.preloadContent(request);
    }

    @Nullable
    VodPlaybackState.PreloadEntry consume(Episode episode) {
        VodPlaybackState.PreloadEntry preload = state.consumePreload(host.getVodKey(), state.getFlag(), episode);
        if (preload == null) clear();
        return preload;
    }

    boolean seek(Episode episode) {
        VodPlaybackState.PreloadEntry preload = state.getPreload();
        return preload != null && !preload.isPending() && preload.request().matches(host.getVodKey(), state.getFlag(), episode) && host.seekToPreloaded(preload.request());
    }

    void onResult(PlaybackResult<VodPlayRequest> preload) {
        VodPlaybackState.PreloadEntry pending = state.getPreload();
        if (preload == null || pending == null || !pending.isPending() || pending.request() != preload.request()) return;
        apply(preload.request(), preload.result());
    }

    void onError(VodPlayRequest request) {
        VodPlaybackState.PreloadEntry preload = state.getPreload();
        if (preload != null && preload.request() == request) clear();
    }

    void clear() {
        waiting = false;
        playedMs = 0;
        playingSinceMs = C.TIME_UNSET;
        dataSource.cancelPreloadContent();
        state.clearPreload();
        host.clearPreload();
    }

    @Nullable
    private Episode findNextEpisode() {
        if (!state.hasEpisode() || !host.canPreloadNext()) return null;
        History history = state.getHistory();
        int offset = history != null && history.isRevPlay() ? -1 : 1;
        Episode episode = state.getRelativeEpisode(offset);
        return episode.isSelected() ? null : episode;
    }

    private void apply(VodPlayRequest request, Result result) {
        if (result != null && !result.getUrl().getValues().isEmpty()) result.getUrl().set(request.getQualityPosition());
        Episode episode = state.findEpisode(host.getVodKey(), request);
        if (isPreloadable(request, result, episode)) start(request, result, episode);
        else clear();
    }

    private boolean isPreloadable(VodPlayRequest request, Result result, Episode episode) {
        return episode != null && host.canPreloadNext() && request.accepts(result) && !result.hasMsg() && !result.needParse() && !result.isUseParse() && result.getDrm() == null && !result.getRealUrl().isEmpty() && Source.get().canPreload(result.getUrl().uri());
    }

    private void start(VodPlayRequest request, Result result, Episode episode) {
        MediaMetadata metadata = VodPlaybackMedia.metadata(state.getHistory(), episode);
        long position = Math.max(0, VodSkipPolicy.startPositionMs(state.getHistory(), result, C.TIME_UNSET));
        if (host.preloadPlayback(result, position, metadata, request)) state.completePreload(result);
        else clear();
    }
}
