package com.fongmi.android.tv.playback.vod;

import androidx.media3.common.C;
import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.SiteApi;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.SkipSegment;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.playback.PlaybackResult;
import com.fongmi.android.tv.player.extractor.Source;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.Collections;
import java.util.List;

public class VodPlaybackController {

    private static final String PUSH_PREFIX = "push://";
    private static final String SEARCH_PREFIX = "msearch:";

    private final VodPlaybackHost host;
    private final VodDataSource dataSource;
    private final VodPlaybackState state;
    private final VodHistoryPolicy historyPolicy;
    private final VodFallbackPolicy fallbackPolicy;
    private final VodPreloader preloader;
    private History lastHistory;
    private SkipSegment activeSkip;
    private boolean manualEndingTriggered;

    public VodPlaybackController(VodPlaybackHost host, VodDataSource dataSource, VodPlaybackState state) {
        this.host = host;
        this.dataSource = dataSource;
        this.state = state;
        this.historyPolicy = new VodHistoryPolicy();
        this.fallbackPolicy = new VodFallbackPolicy(this, state, host, dataSource);
        this.preloader = new VodPreloader(host, dataSource, state);
    }

    public void reset() {
        dataSource.cancelContent();
        preloader.clear();
        state.reset();
        clearSkipState();
    }

    private void clearSkipState() {
        activeSkip = null;
        manualEndingTriggered = false;
    }

    public void checkId() {
        String id = resolveVodId();
        if (id.isEmpty() || id.startsWith(SEARCH_PREFIX)) detailEmpty(false);
        else if (!state.isDetailRequested(host.getVodKey(), id)) requestDetail();
    }

    private String resolveVodId() {
        String id = host.getVodId();
        if (!Source.isPush(id)) return id;
        host.usePushId(id);
        return host.getVodId();
    }

    public void requestDetail() {
        String key = host.getVodKey();
        String id = host.getVodId();
        state.setDetailRequest(key, id);
        state.setBuiltin(SiteApi.PUSH.equals(key) && Source.isBuiltin(id));
        dataSource.detailContent(key, id, state.isBuiltin());
    }

    public void onDetailResult(VodDetailResult detail) {
        if (detail == null || !detail.matches(host.getVodKey(), host.getVodId())) return;
        Result result = detail.result();
        if (result.getList().isEmpty()) detailEmpty(result.hasMsg());
        else detailLoaded(result.getVod());
        host.showDetailMessage(result.getMsg());
    }

    private void updateVod(Vod item) {
        History history = state.getHistory();
        if (history == null) return;
        replaceVodId(history, item.getId());
        mergeFlags(item.getFlags());
        if (!item.getName().isEmpty()) item.checkName(history.getVodName());
        updateHistory(history, item);
        host.renderVodUpdate(item);
        publishActivePlaybackMetadata();
    }

    private void updateHistory(History history, Vod item) {
        String pic = item.getPic();
        String name = item.getName();
        boolean hasPic = !pic.isEmpty();
        boolean hasName = !name.isEmpty();
        if (hasPic) history.setVodPic(pic);
        if (hasName) history.setVodName(name);
        if (hasName || hasPic) historyPolicy.saveCurrent(history);
    }

    private void publishActivePlaybackMetadata() {
        Episode episode = findEpisode(state.getActiveRequest());
        if (episode != null) publishPlaybackMetadata(episode);
    }

    private void replaceVodId(History history, String id) {
        if (id.isEmpty()) return;
        if (Source.isPush(host.getVodId()) && !Source.isPush(id)) id = PUSH_PREFIX + id;
        if (id.equals(host.getVodId())) return;
        String oldKey = host.getHistoryKey();
        preloader.clear();
        host.setVodId(id);
        String newKey = host.getHistoryKey();
        history.replace(newKey);
        Keep.replace(oldKey, newKey);
    }

    public void onPlaybackResult(PlaybackResult<VodPlayRequest> playback) {
        if (playback == null || cannotApply(playback)) return;
        applyPlaybackResult(playback.result(), playback.request());
    }

    private boolean redirectPush(Result result) {
        if (result.hasMsg() || !Source.isPush(result.getUrl().v())) return false;
        redirectPush(result.getUrl().v());
        return true;
    }

    public void redirectPush(String url) {
        String target = url.substring(PUSH_PREFIX.length());
        dataSource.cancelContent();
        preloader.clear();
        state.clearPlayRequest();
        String id = host.getVodId();
        boolean sameVideo = SiteApi.PUSH.equals(host.getVodKey()) && Source.isPush(id) && id.substring(PUSH_PREFIX.length()).equals(target);
        if (target.isEmpty() || sameVideo) playbackError(ResUtil.getString(R.string.error_play_url));
        else host.openPush(target);
    }

    private void applyPlaybackResult(Result result, VodPlayRequest request) {
        Episode episode = findEpisode(request);
        if (episode == null) return;
        result.getUrl().set(request.getQualityPosition());
        if (redirectPush(result)) return;
        applyPlaybackState(result, request);
        renderPlaybackResult(result, episode);
        startPlayback(result, request.getStartPositionMs() == C.TIME_UNSET ? startPositionMs() : request.getStartPositionMs(), episode);
        preloader.update(result);
    }

    private void renderPlaybackResult(Result result, Episode episode) {
        host.renderUseParse(state.isUseParse());
        host.renderQuality(result, result.getUrl().isMulti());
        if (result.hasDesc()) host.renderDescription(result.getDesc());
        if (result.hasArtwork()) host.renderArtwork(result.getArtwork());
        host.loadDanmaku(result, state.getHistory(), episode);
    }

    private void applyPlaybackState(Result result, VodPlayRequest request) {
        clearSkipState();
        state.setQuality(result);
        state.setPlayingRequest(request);
        state.setUseParse(result.isUseParse());
        state.setQualityPosition(request.getQualityPosition());
        if (request.getStartPositionMs() != C.TIME_UNSET) state.getHistory().setPosition(request.getStartPositionMs());
        else if (result.hasPosition()) state.getHistory().setPosition(result.getPosition());
        if (!result.getList().isEmpty()) updateVod(result.getVod());
    }

    private void startPlayback(Result result, long startPositionMs, Episode episode) {
        MediaMetadata metadata = publishPlaybackMetadata(episode);
        host.setPlaybackEnding(state.getHistory() == null ? 0 : state.getHistory().getEnding());
        host.startPlayback(result, state.isUseParse(), startPositionMs, metadata);
    }

    public void onSearchResult(Result result) {
        fallbackPolicy.onSearchResult(result);
    }

    public void selectFlag(Flag item) {
        selectFlag(item, false);
    }

    private void selectFlag(Flag item, boolean force) {
        if (!state.hasFlags()) return;
        Flag selected = resolveFlag(item);
        if (!force && selected.isSelected()) return;
        preloader.clear();
        for (Flag flag : state.getFlags()) flag.setSelected(selected);
        host.renderFlagSelection(selected);
        host.renderEpisodes(selected.getEpisodes());
        host.renderQualityVisible(false);
        seamless(selected);
    }

    public void selectEpisode(Episode item) {
        if (!state.hasFlags()) return;
        if (preloader.seek(item)) return;
        saveCurrentHistory();
        Flag selected = state.getFlag();
        for (Flag flag : state.getFlags()) flag.toggle(flag == selected, item);
        historyPolicy.updateEpisode(state.getHistory(), state.getFlag(), item);
        host.renderEpisodeSelection(item);
        playEpisode(item);
    }

    public void onPreloadError(VodPlayRequest request) {
        preloader.onError(request);
    }

    public void onPreloadTransition(VodPlayRequest request, long oldPositionMs, long oldDurationMs) {
        VodPlaybackState.PreloadEntry preload = state.getPreload();
        if (preload == null || preload.isPending() || preload.request() != request) return;
        Flag selected = state.findFlag(host.getVodKey(), request);
        Episode episode = state.findEpisode(host.getVodKey(), selected, request);
        if (episode == null) return;
        saveHistory(false, System.currentTimeMillis(), oldPositionMs, oldDurationMs);
        state.consumePreload(host.getVodKey(), selected, episode);
        for (Flag flag : state.getFlags()) flag.toggle(flag == selected, episode);
        historyPolicy.updateEpisode(state.getHistory(), selected, episode);
        host.renderEpisodeSelection(episode);
        applyPlaybackState(preload.result(), request);
        renderPlaybackResult(preload.result(), episode);
        publishPlaybackMetadata(episode);
        preloader.update(preload.result());
    }

    private void playEpisode(Episode item) {
        VodPlaybackState.PreloadEntry preload = preloader.consume(item);
        host.stopPlaybackForRefresh();
        if (preload == null) {
            requestSelectedEpisode();
            return;
        }
        applyPlaybackResult(preload.result(), preload.request());
    }

    public void selectQuality(Result result) {
        if (!state.hasEpisode()) return;
        if (redirectPush(result)) return;
        long position = host.getPlayerPosition();
        saveCurrentHistory();
        state.setQualityPosition(result.getUrl().getPosition());
        preloader.clear();
        host.stopPlaybackForRefresh();
        requestPlayer(state.getFlag(), state.getEpisode(), position);
    }

    public void selectParse(Parse item) {
        VodConfig.get().setParse(item);
        refresh();
    }

    private void mergeFlags(List<Flag> items) {
        if (items.isEmpty()) return;
        if (state.hasFlags()) {
            Flag activated = state.getFlag();
            for (Flag item : items) mergeFlag(activated, item);
        } else {
            state.setFlags(items);
        }
        host.renderFlags(state.getFlags());
    }

    public void selectSource(Vod item) {
        switchSource(item, false);
    }

    void fallbackSource(Vod item) {
        switchSource(item, true);
    }

    private void switchSource(Vod item, boolean autoFallback) {
        state.setAutoFallback(autoFallback);
        saveCurrentHistory();
        preloader.clear();
        state.clearPlayRequest();
        host.prepareSource(item);
        requestDetail();
    }

    public void search(String keyword) {
        fallbackPolicy.search(keyword, false);
    }

    public void manualSwitchSource() {
        fallbackPolicy.manualSwitchSource();
    }

    public void playbackError(String msg) {
        preloader.clear();
        host.resetPlaybackForError(msg);
        fallbackPolicy.playbackError();
    }

    public void playbackEnded() {
        nextEpisode(true);
    }

    public void replay() {
        if (state.getHistory() != null) state.getHistory().setPosition(C.TIME_UNSET);
        if (host.isPlayerEmpty()) refresh();
        else host.replay(startPositionMs());
    }

    public void refresh() {
        saveCurrentHistory();
        preloader.clear();
        host.stopPlaybackForRefresh();
        restorePlaybackSelection(state.getPlayingRequest());
        requestSelectedEpisode();
    }

    public void onPlaybackServiceReady() {
        MediaMetadata metadata = state.getPlaybackMetadata();
        if (metadata != null) host.renderPlaybackMetadata(metadata);
        VodPlayRequest request = state.getPlayingRequest();
        if (request == null) return;
        if (host.hasPlaybackSession()) {
            if (state.getPreload() == null) preloader.update(state.getQuality());
            return;
        }
        restorePlaybackSelection(request);
        requestSelectedEpisode();
    }

    private void requestSelectedEpisode() {
        if (state.hasEpisode()) requestPlayer(state.getFlag(), state.getEpisode(), C.TIME_UNSET);
    }

    public void nextEpisode(boolean notify) {
        boolean reversed = state.getHistory() != null && state.getHistory().isRevPlay();
        moveEpisode(reversed ? -1 : 1, notify, reversed);
    }

    public void prevEpisode(boolean notify) {
        boolean reversed = state.getHistory() != null && state.getHistory().isRevPlay();
        moveEpisode(reversed ? 1 : -1, notify, reversed);
    }

    private void moveEpisode(int offset, boolean notify, boolean reversed) {
        if (!state.hasEpisode()) return;
        Episode item = state.getRelativeEpisode(offset);
        if (!item.isSelected()) selectEpisode(item);
        else if (notify && offset > 0) host.showNoNext(reversed);
        else if (notify) host.showNoPrev(reversed);
    }

    public void reverseEpisode(boolean scroll) {
        if (!state.hasFlags()) return;
        for (Flag flag : state.getFlags()) reverseEpisodes(flag);
        host.renderReverseEpisodes(state.getFlag().getEpisodes(), scroll);
        if (state.getPlayingRequest() != null) preloader.update(state.getQuality());
    }

    private void reverseEpisodes(Flag flag) {
        List<Episode> episodes = flag.getEpisodes();
        int position = flag.getPosition();
        Collections.reverse(episodes);
        if (position >= 0 && position < episodes.size()) flag.setPosition(episodes.size() - position - 1);
    }

    private void saveCurrentHistory() {
        if (state.getPlayingRequest() == null || !host.canTrackPlaybackProgress()) historyPolicy.save(currentHistory());
        else saveHistory(false, System.currentTimeMillis(), host.getPlayerPosition(), host.getPlayerDuration());
    }

    public void saveHistory(boolean exit, long time, long position, long duration) {
        History history = exit ? historyForExit() : currentHistory();
        if (host.isLivePlayback()) historyPolicy.saveVisit(history, exit, time);
        else if (host.isIsoNavigationPlayback()) historyPolicy.saveDiscVisit(history, exit, time);
        else historyPolicy.saveProgress(history, exit, time, position, duration);
    }

    public void onPlayingChanged(boolean playing) {
        preloader.onPlayingChanged(playing);
    }

    public void onPlaybackSeek() {
        preloader.onPlayingChanged(host.isPlayerPlaying());
    }

    public void onTimeChanged(long time, long position, long duration) {
        VodPlayRequest request = state.getPlayingRequest();
        if (request == null) return;
        preloader.onTimeChanged();
        History history = currentHistory();
        host.setPlaybackEnding(history == null ? 0 : history.getEnding());
        historyPolicy.updateProgress(history, time, position, duration);
        if (history != null && duration > 0 && history.getEnding() > 0 && history.getEnding() + position >= duration) {
            skipEnding(history, duration, request);
            return;
        }
        manualEndingTriggered = false;
        skipSegment(history, position, duration, request);
    }

    private void skipEnding(History history, long duration, VodPlayRequest request) {
        if (!host.isPlaybackEndingManaged() && !manualEndingTriggered) {
            boolean triggered = skipToEnd(history, duration);
            if (state.getPlayingRequest() == request) manualEndingTriggered = triggered;
        }
    }

    private void skipSegment(History history, long position, long duration, VodPlayRequest request) {
        SkipSegment segment = VodSkipPolicy.activeSegment(history, state.getQuality(), position);
        if (segment == null) {
            activeSkip = null;
            return;
        }
        if (segment == activeSkip) return;
        if (!segment.isToEnd()) {
            activeSkip = segment;
            host.seekPlayback(segment.getEnd());
            return;
        }
        if (skipToEnd(history, duration) && state.getPlayingRequest() == request) activeSkip = segment;
    }

    private boolean skipToEnd(History history, long duration) {
        if (!state.hasEpisode()) return false;
        boolean reversed = history != null && history.isRevPlay();
        if (!state.getRelativeEpisode(reversed ? -1 : 1).isSelected()) {
            nextEpisode(false);
            return true;
        }
        if (duration <= 0) return false;
        host.seekPlayback(duration);
        return true;
    }

    public long startPositionMs() {
        History history = state.getHistory();
        return VodSkipPolicy.startPositionMs(history, state.getQuality(), history == null ? C.TIME_UNSET : history.getPosition());
    }

    public long openingPositionMs() {
        return Math.max(0, VodSkipPolicy.startPositionMs(state.getHistory(), state.getQuality(), C.TIME_UNSET));
    }

    private History currentHistory() {
        History history = state.getHistory();
        if (history != null) lastHistory = history;
        return history;
    }

    private History historyForExit() {
        History history = currentHistory();
        return history == null ? lastHistory : history;
    }

    public void onPreloadResult(PlaybackResult<VodPlayRequest> preload) {
        preloader.onResult(preload);
    }

    public void setOpening(long opening) {
        if (state.getHistory() != null) state.getHistory().setOpening(opening);
        activeSkip = null;
        if (state.getPlayingRequest() != null) preloader.update(state.getQuality());
    }

    public void setEnding(long ending) {
        if (state.getHistory() != null) state.getHistory().setEnding(ending);
        clearSkipState();
        host.setPlaybackEnding(ending);
    }

    public void setScale(int scale) {
        if (state.getHistory() != null) state.getHistory().setScale(scale);
    }

    public void setRevSort(boolean revSort) {
        if (state.getHistory() != null) state.getHistory().setRevSort(revSort);
    }

    public void setRevPlay(boolean revPlay) {
        if (state.getHistory() != null) state.getHistory().setRevPlay(revPlay);
        if (state.getPlayingRequest() != null) preloader.update(state.getQuality());
    }

    private void detailEmpty(boolean shouldFinish) {
        if (shouldFinish || host.isFromCollect()) {
            host.finishVod();
            return;
        }
        String name = host.getVodName();
        if (name.isEmpty()) {
            host.renderEmptyDetail();
        } else {
            host.renderFallbackName(name);
            host.onDetailFallbackScheduled();
            fallbackPolicy.emptyDetail();
        }
    }

    private void detailLoaded(Vod item) {
        VodPlayRequest activeRequest = state.getActiveRequest();
        History history = applyDetail(item);
        if (state.hasFlags()) restoreDetail(activeRequest, history);
        else fallbackPolicy.emptyFlag();
    }

    private History applyDetail(Vod item) {
        item.checkPic(host.getVodPic());
        item.checkName(host.getVodName());
        List<Flag> flags = item.getFlags();
        state.setFlags(flags);
        History history = historyPolicy.findOrCreate(host.getHistoryKey(), host.getVodMark(), item);
        state.setHistory(history);
        lastHistory = history;
        host.renderDetail(item, history);
        host.renderFlags(flags);
        host.renderHistory(history);
        host.onDetailFallbackCancelled();
        return history;
    }

    private void restoreDetail(VodPlayRequest activeRequest, History history) {
        if (restorePlaybackSelection(activeRequest)) publishPlaybackMetadata(state.getEpisode());
        else selectFlag(history.getFlag(), true);
        if (history.isRevSort()) reverseEpisode(true);
        if (state.getPlayingRequest() != null && !host.hasPlaybackSession()) requestSelectedEpisode();
    }

    private boolean restorePlaybackSelection(VodPlayRequest request) {
        Flag flag = state.findFlag(host.getVodKey(), request);
        Episode episode = state.findEpisode(host.getVodKey(), flag, request);
        if (flag == null || episode == null) return false;
        for (Flag item : state.getFlags()) item.setSelected(flag);
        for (Flag item : state.getFlags()) item.toggle(item == flag, episode);
        historyPolicy.updateEpisode(state.getHistory(), flag, episode);
        host.renderFlagSelection(flag);
        host.renderEpisodes(flag.getEpisodes());
        host.renderEpisodeSelection(episode);
        host.renderQualityVisible(state.getQuality().getUrl().isMulti());
        return true;
    }

    private void requestPlayer(Flag flag, Episode episode, long startPositionMs) {
        clearSkipState();
        historyPolicy.updateEpisode(state.getHistory(), flag, episode);
        VodPlayRequest request = VodPlayRequest.create(host.getVodKey(), flag, episode, state.isBuiltin(), state.getQualityPosition(), startPositionMs);
        state.setPendingRequest(request);
        publishPlaybackMetadata(episode);
        dataSource.playerContent(request);
        host.onPlaybackRequested();
    }

    private Episode findEpisode(VodPlayRequest request) {
        return state.findEpisode(host.getVodKey(), request);
    }

    private MediaMetadata publishPlaybackMetadata(Episode episode) {
        VodPlaybackInfo.update(state.getHistory(), state.getFlag(), episode);
        MediaMetadata metadata = VodPlaybackMedia.metadata(state.getHistory(), episode);
        state.setPlaybackMetadata(metadata);
        host.renderPlaybackMetadata(metadata);
        return metadata;
    }

    private void seamless(Flag flag) {
        History history = state.getHistory();
        Episode episode = history == null ? null : flag.find(history.getVodRemarks(), host.getVodMark().isEmpty());
        host.renderQualityVisible(episode != null && episode.isSelected() && state.getQuality().getUrl().isMulti());
        if (episode == null || episode.isSelected()) return;
        history.setVodRemarks(episode.getName());
        selectEpisode(episode);
    }

    private void mergeFlag(Flag activated, Flag item) {
        Flag target = findFlag(item);
        if (target == null) {
            state.getFlags().add(item);
        } else {
            target.mergeEpisodes(item.getEpisodes(), state.getHistory() != null && state.getHistory().isRevSort());
            if (target.equals(activated)) host.renderEpisodes(target.getEpisodes());
        }
    }

    private Flag resolveFlag(Flag item) {
        Flag flag = findFlag(item);
        return flag == null ? state.getFlags().get(0) : flag;
    }

    private Flag findFlag(Flag item) {
        if (item != null) for (Flag flag : state.getFlags()) if (flag.equals(item)) return flag;
        return null;
    }

    private boolean cannotApply(PlaybackResult<VodPlayRequest> playback) {
        VodPlayRequest pending = state.getPendingRequest();
        VodPlayRequest request = playback.request();
        if (host.isHostFinishing() || pending == null || request == null) return true;
        return pending != request || findEpisode(request) == null || !request.accepts(playback.result());
    }
}
