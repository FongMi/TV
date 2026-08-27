package com.fongmi.android.tv.playback.vod;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
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

    boolean isRequestPending() {
        return state.getPreloadRequest() != null && state.getPreloadResult() == null;
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
    Result consume(Episode episode) {
        Result result = state.consumePreload(host.getVodKey(), state.getFlag(), episode);
        if (result == null) clear();
        return result;
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
        VodPlayRequest pending = state.getPreloadRequest();
        return preload != null && pending != null && pending.matches(preload.request()) && state.getPreloadResult() == null;
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
        if (host.preloadPlayback(result, getStartPositionMs(result), metadata)) state.completePreload(result);
        else clear();
    }

    private long getStartPositionMs(Result result) {
        History history = state.getHistory();
        long opening = history == null ? C.TIME_UNSET : history.getOpening();
        long position = result.hasPosition() ? result.getPosition() : C.TIME_UNSET;
        return Math.max(0, Math.max(opening, position));
    }
}
