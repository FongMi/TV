package com.fongmi.android.tv.playback.vod;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.playback.PlaybackResult;

final class VodPreloader {

    private final VodPlaybackHost host;
    private final VodDataSource dataSource;
    private final VodPlaybackState state;

    VodPreloader(VodPlaybackHost host, VodDataSource dataSource, VodPlaybackState state) {
        this.host = host;
        this.dataSource = dataSource;
        this.state = state;
    }

    boolean deferUpdate(Vod item) {
        VodPlaybackState.PreloadEntry preload = state.getPreload();
        if (preload != null && preload.isPending()) {
            state.setPreloadUpdate(item);
            return true;
        }
        if (preload != null) state.setPreloadUpdate(null);
        return false;
    }

    void update(Result result) {
        if (result.needParse() || state.isUseParse()) clear();
        else preloadNext();
    }

    void preloadNext() {
        Episode episode = findNextEpisode();
        if (episode == null) clear();
        else request(episode);
    }

    @Nullable
    VodPlaybackState.PreloadEntry consume(Episode episode) {
        VodPlaybackState.PreloadEntry preload = state.consumePreload(host.getVodKey(), state.getFlag(), episode);
        if (preload == null) clear();
        return preload;
    }

    void onResult(PlaybackResult<VodPlayRequest> preload) {
        if (!isPending(preload)) return;
        apply(preload.request(), preload.result());
    }

    void clear() {
        state.clearPreload();
        host.clearPreload();
    }

    private void request(Episode episode) {
        VodPlayRequest request = VodPlayRequest.create(host.getVodKey(), state.getFlag(), episode);
        state.beginPreload(request);
        dataSource.preloadContent(request);
    }

    @Nullable
    private Episode findNextEpisode() {
        if (!state.hasEpisode() || !host.canPreloadNext()) return null;
        History history = state.getHistory();
        int offset = history != null && history.isRevPlay() ? -1 : 1;
        Episode episode = state.getRelativeEpisode(offset);
        return episode.isSelected() ? null : episode;
    }

    private boolean isPending(PlaybackResult<VodPlayRequest> preload) {
        VodPlaybackState.PreloadEntry pending = state.getPreload();
        return preload != null && pending != null && pending.isPending() && pending.request().matches(preload.request());
    }

    private void apply(VodPlayRequest request, Result result) {
        Episode episode = state.findEpisode(host.getVodKey(), request);
        if (isPreloadable(request, result, episode)) start(result, episode);
        else clear();
    }

    private boolean isPreloadable(VodPlayRequest request, Result result, Episode episode) {
        return episode != null && host.canPreloadNext() && request.accepts(result) && !result.hasMsg() && !result.needParse() && !result.isUseParse() && result.getDrm() == null && !result.getRealUrl().isEmpty();
    }

    private void start(Result result, Episode episode) {
        result.getUrl().set(state.getQualityPosition());
        MediaMetadata metadata = VodPlaybackMedia.metadata(state.getHistory(), episode);
        long position = Math.max(0, VodSkipPolicy.startPositionMs(state.getHistory(), result, C.TIME_UNSET));
        if (host.preloadPlayback(result, position, metadata)) state.completePreload(result);
        else clear();
    }
}
