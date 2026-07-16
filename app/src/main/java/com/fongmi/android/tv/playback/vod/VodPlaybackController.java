package com.fongmi.android.tv.playback.vod;

import androidx.media3.common.C;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;

import java.util.Collections;
import java.util.List;

public class VodPlaybackController {

    private static final String PUSH_PREFIX = "push://";
    private static final String SEARCH_PREFIX = "msearch:";

    private final VodHistoryPolicy historyPolicy;
    private final VodFallbackPolicy fallbackPolicy;
    private final VodPlaybackState state;
    private final VodPlaybackHost host;
    private History lastHistory;

    public VodPlaybackController(VodPlaybackHost host, VodPlaybackState state) {
        this.historyPolicy = new VodHistoryPolicy();
        this.state = state;
        this.host = host;
        this.fallbackPolicy = new VodFallbackPolicy(this, state, host);
    }

    public void reset() {
        state.reset();
    }

    public void checkId() {
        String id = resolveVodId();
        if (id.isEmpty() || id.startsWith(SEARCH_PREFIX)) detailEmpty(false);
        else requestDetail();
    }

    private String resolveVodId() {
        String id = host.getVodId();
        if (!id.startsWith(PUSH_PREFIX)) return id;
        host.usePushId(id.substring(PUSH_PREFIX.length()));
        return host.getVodId();
    }

    public void requestDetail() {
        host.requestDetail(host.getVodKey(), host.getVodId());
    }

    public void onDetailResult(Result result) {
        if (result.getList().isEmpty()) detailEmpty(result.hasMsg());
        else detailLoaded(result.getVod());
        host.showDetailMessage(result.getMsg());
    }

    public void updateVod(Vod item) {
        History history = state.getHistory();
        String id = item.getId();
        String pic = item.getPic();
        String name = item.getName();
        boolean hasPic = !pic.isEmpty();
        boolean hasName = !name.isEmpty();
        replaceVodId(history, id);
        mergeFlags(item.getFlags());
        if (hasPic) history.setVodPic(pic);
        if (hasName) history.setVodName(name);
        if (hasName || hasPic) historyPolicy.saveCurrent(history);
        host.renderVodUpdate(item);
    }

    private void replaceVodId(History history, String id) {
        if (id.isEmpty() || id.equals(host.getVodId())) return;
        String oldKey = host.getHistoryKey();
        host.setVodId(id);
        String newKey = host.getHistoryKey();
        history.replace(newKey);
        Keep.replace(oldKey, newKey);
    }

    public void onPlayerResult(Result result) {
        VodPlayRequest request = state.getPendingRequest();
        if (request == null) request = currentRequest();
        if (cannotApply(result, request)) return;
        applyPlayerResult(result, request);
    }

    private void applyPlayerResult(Result result, VodPlayRequest request) {
        state.setQuality(result);
        state.setPlayingRequest(request);
        state.setUseParse(result.isUseParse());
        host.renderUseParse(state.isUseParse());
        result.getUrl().set(state.getQualityPosition());
        host.renderQuality(result, result.getUrl().isMulti());
        if (result.hasDesc()) host.renderDescription(result.getDesc());
        if (result.hasArtwork()) host.renderArtwork(result.getArtwork());
        if (result.hasPosition()) state.getHistory().setPosition(result.getPosition());
        startPlayback(result, startPositionMs());
        host.loadDanmaku(result, state.getHistory(), state.getEpisode());
    }

    private void startPlayback(Result result, long startPositionMs) {
        host.startPlayback(result, state.isUseParse(), startPositionMs, state.getHistory(), state.getEpisode());
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
        for (Flag flag : state.getFlags()) flag.setSelected(selected);
        host.renderFlagSelection(selected);
        host.renderEpisodes(selected.getEpisodes());
        host.renderQualityVisible(false);
        seamless(selected);
    }

    public void selectEpisode(Episode item) {
        if (!state.hasFlags()) return;
        saveCurrentHistory();
        Flag selected = state.getFlag();
        for (Flag flag : state.getFlags()) flag.toggle(flag == selected, item);
        historyPolicy.updateEpisode(state.getHistory(), state.getFlag(), item);
        host.renderEpisodeSelection(item);
        if (host.isFullscreenForPlayback()) host.showEpisodeReady(item);
        restartPlayback();
    }

    public void selectQuality(Result result) {
        if (!state.hasEpisode()) return;
        state.setQuality(result);
        state.setQualityPosition(result.getUrl().getPosition());
        startPlayback(result, host.getPlayerPosition());
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
        restartPlayback();
    }

    private void restartPlayback() {
        host.stopPlaybackForRefresh();
        if (!state.hasEpisode()) return;
        requestPlayer(state.getFlag(), state.getEpisode());
    }

    public void nextEpisode(boolean notify) {
        if (state.getHistory() != null && state.getHistory().isRevPlay()) prevEpisode(notify, true);
        else nextEpisode(notify, false);
    }

    public void prevEpisode(boolean notify) {
        if (state.getHistory() != null && state.getHistory().isRevPlay()) nextEpisode(notify, true);
        else prevEpisode(notify, false);
    }

    private void nextEpisode(boolean notify, boolean reversed) {
        if (!state.hasEpisode()) return;
        Episode item = getRelativeEpisode(1);
        if (!item.isSelected()) selectEpisode(item);
        else if (notify) host.showNoNext(reversed);
    }

    private void prevEpisode(boolean notify, boolean reversed) {
        if (!state.hasEpisode()) return;
        Episode item = getRelativeEpisode(-1);
        if (!item.isSelected()) selectEpisode(item);
        else if (notify) host.showNoPrev(reversed);
    }

    public void reverseEpisode(boolean scroll) {
        if (!state.hasFlags()) return;
        for (Flag flag : state.getFlags()) Collections.reverse(flag.getEpisodes());
        host.renderReverseEpisodes(state.getFlag().getEpisodes(), scroll);
    }

    private void saveCurrentHistory() {
        if (state.getPlayingRequest() == null || !host.canTrackPlaybackProgress()) historyPolicy.save(currentHistory());
        else saveHistory(false, System.currentTimeMillis(), host.getPlayerPosition(), host.getPlayerDuration());
    }

    public void saveHistory(boolean exit, long time, long position, long duration) {
        History history = exit ? historyForExit() : currentHistory();
        if (host.isLivePlayback()) historyPolicy.saveVisit(history, exit, time);
        else historyPolicy.saveProgress(history, exit, time, position, duration);
    }

    public void onTimeChanged(long time, long position, long duration) {
        History history = currentHistory();
        historyPolicy.updateProgress(history, time, position, duration);
        if (history != null && history.getEnding() > 0 && history.getEnding() + position >= duration) nextEpisode(false);
    }

    public long startPositionMs() {
        return historyPolicy.startPositionMs(state.getHistory());
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

    public void setOpening(long opening) {
        if (state.getHistory() != null) state.getHistory().setOpening(opening);
    }

    public void setEnding(long ending) {
        if (state.getHistory() != null) state.getHistory().setEnding(ending);
    }

    public void setScale(int scale) {
        if (state.getHistory() != null) state.getHistory().setScale(scale);
    }

    public void setRevSort(boolean revSort) {
        if (state.getHistory() != null) state.getHistory().setRevSort(revSort);
    }

    public void setRevPlay(boolean revPlay) {
        if (state.getHistory() != null) state.getHistory().setRevPlay(revPlay);
    }

    private void detailEmpty(boolean shouldFinish) {
        if (host.isFromCollect() || shouldFinish) {
            host.finishVod();
            return;
        }
        String name = host.getVodName();
        if (name.isEmpty()) host.renderEmptyDetail();
        else fallbackDetail(name);
    }

    private void fallbackDetail(String name) {
        host.renderFallbackName(name);
        host.onDetailFallbackScheduled();
        fallbackPolicy.emptyDetail();
    }

    private void detailLoaded(Vod item) {
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
        if (flags.isEmpty()) {
            fallbackPolicy.emptyFlag();
        } else {
            selectFlag(history.getFlag(), true);
            if (history.isRevSort()) reverseEpisode(true);
        }
    }

    private void requestPlayer(Flag flag, Episode episode) {
        historyPolicy.updateEpisode(state.getHistory(), flag, episode);
        VodPlayRequest request = VodPlayRequest.create(host.getVodKey(), flag, episode);
        state.setPendingRequest(request);
        host.requestPlayer(request);
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
        if (flag != null) return flag;
        return state.getFlags().get(0);
    }

    private Flag findFlag(Flag item) {
        if (item != null) for (Flag flag : state.getFlags()) if (flag.equals(item)) return flag;
        return null;
    }

    private boolean cannotApply(Result result, VodPlayRequest request) {
        if (host.isHostFinishing() || !state.hasEpisode() || request == null) return true;
        return !request.matches(host.getVodKey(), state.getFlag(), state.getEpisode()) || !request.accepts(result);
    }

    private VodPlayRequest currentRequest() {
        return state.hasEpisode() ? VodPlayRequest.create(host.getVodKey(), state.getFlag(), state.getEpisode()) : null;
    }

    private Episode getRelativeEpisode(int offset) {
        List<Episode> episodes = state.getFlag().getEpisodes();
        int current = state.getFlag().getPosition();
        int position = Math.clamp(current + offset, 0, episodes.size() - 1);
        return episodes.get(position);
    }
}
