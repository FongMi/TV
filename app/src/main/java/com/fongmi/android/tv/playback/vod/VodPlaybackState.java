package com.fongmi.android.tv.playback.vod;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class VodPlaybackState {

    private final Set<String> failedIds;
    private final List<Vod> sources;
    private final List<Flag> flags;
    private VodPlayRequest pendingRequest;
    private VodPlayRequest playingRequest;
    private MediaMetadata playbackMetadata;
    private VodPlayRequest preloadRequest;
    private Result preloadResult;
    private Result quality;
    private History history;
    private String detailKey;
    private String detailId;
    private String searchKeyword;
    private int qualityPosition;
    private boolean selectFirstSource;
    private boolean autoFallback;
    private boolean useParse;

    public VodPlaybackState() {
        this.failedIds = new HashSet<>();
        this.sources = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.quality = Result.empty();
        this.detailKey = "";
        this.detailId = "";
        this.searchKeyword = "";
    }

    public void reset() {
        failedIds.clear();
        sources.clear();
        flags.clear();
        clearPlayRequest();
        playbackMetadata = null;
        clearPreload();
        quality = Result.empty();
        history = null;
        detailKey = "";
        detailId = "";
        searchKeyword = "";
        qualityPosition = 0;
        selectFirstSource = false;
        autoFallback = false;
        useParse = false;
    }

    void addFailedId(String id) {
        if (id != null && !id.isEmpty()) failedIds.add(id);
    }

    boolean hasFailedId(String id) {
        return id != null && failedIds.contains(id);
    }

    List<Vod> getSources() {
        return sources;
    }

    void setSources(List<Vod> items) {
        sources.clear();
        sources.addAll(items);
    }

    Vod removeFirstSource() {
        return sources.remove(0);
    }

    boolean hasSources() {
        return !sources.isEmpty();
    }

    List<Flag> getFlags() {
        return flags;
    }

    void setFlags(List<Flag> items) {
        flags.clear();
        flags.addAll(items);
    }

    boolean hasFlags() {
        return !flags.isEmpty();
    }

    int getFlagPosition() {
        for (int i = 0; i < flags.size(); i++) if (flags.get(i).isSelected()) return i;
        return 0;
    }

    Flag getFlag() {
        return flags.get(getFlagPosition());
    }

    Episode getEpisode() {
        Flag flag = getFlag();
        int position = flag.getPosition();
        return flag.getEpisodes().get(position >= 0 && position < flag.getEpisodes().size() ? position : 0);
    }

    boolean hasEpisode() {
        return hasFlags() && !getFlag().getEpisodes().isEmpty();
    }

    Episode getRelativeEpisode(int offset) {
        List<Episode> episodes = getFlag().getEpisodes();
        int position = Math.clamp(getFlag().getPosition() + offset, 0, episodes.size() - 1);
        return episodes.get(position);
    }

    @Nullable
    Episode findEpisode(String key, VodPlayRequest request) {
        if (request == null) return null;
        return flags.stream().map(flag -> findEpisode(key, flag, request)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @Nullable
    Episode findEpisode(String key, Flag flag, VodPlayRequest request) {
        if (flag == null || request == null) return null;
        return flag.getEpisodes().stream().filter(episode -> request.matches(key, flag, episode)).findFirst().orElse(null);
    }

    @Nullable
    Flag findFlag(String key, VodPlayRequest request) {
        if (request == null) return null;
        return flags.stream().filter(flag -> findEpisode(key, flag, request) != null).findFirst().orElse(null);
    }

    Result getQuality() {
        return quality;
    }

    void setQuality(Result quality) {
        this.quality = quality;
    }

    int getQualityPosition() {
        return qualityPosition;
    }

    void setQualityPosition(int qualityPosition) {
        this.qualityPosition = qualityPosition;
    }

    History getHistory() {
        return history;
    }

    void setHistory(History history) {
        this.history = history;
    }

    boolean isDetailRequested(String key, String id) {
        return detailKey.equals(key) && detailId.equals(id);
    }

    void setDetailRequest(String key, String id) {
        detailKey = key == null ? "" : key;
        detailId = id == null ? "" : id;
    }

    VodPlayRequest getPendingRequest() {
        return pendingRequest;
    }

    void setPendingRequest(VodPlayRequest pendingRequest) {
        this.pendingRequest = pendingRequest;
        this.playingRequest = null;
    }

    VodPlayRequest getPlayingRequest() {
        return playingRequest;
    }

    void setPlayingRequest(VodPlayRequest playingRequest) {
        this.playingRequest = playingRequest;
        this.pendingRequest = null;
    }

    @Nullable
    VodPlayRequest getActiveRequest() {
        return pendingRequest != null ? pendingRequest : playingRequest;
    }

    void clearPlayRequest() {
        pendingRequest = null;
        playingRequest = null;
    }

    @Nullable
    VodPlayRequest getPreloadRequest() {
        return preloadRequest;
    }

    @Nullable
    Result getPreloadResult() {
        return preloadResult;
    }

    void beginPreload(VodPlayRequest request) {
        preloadRequest = request;
        preloadResult = null;
    }

    void completePreload(Result result) {
        preloadResult = result;
    }

    @Nullable
    Result consumePreload(String key, Flag flag, Episode episode) {
        if (preloadRequest == null || preloadResult == null || !preloadRequest.matches(key, flag, episode)) return null;
        Result result = preloadResult;
        clearPreload();
        return result;
    }

    void clearPreload() {
        preloadRequest = null;
        preloadResult = null;
    }

    @Nullable
    MediaMetadata getPlaybackMetadata() {
        return playbackMetadata;
    }

    void setPlaybackMetadata(MediaMetadata playbackMetadata) {
        this.playbackMetadata = playbackMetadata;
    }

    boolean isSelectFirstSource() {
        return selectFirstSource;
    }

    void setSelectFirstSource(boolean selectFirstSource) {
        this.selectFirstSource = selectFirstSource;
    }

    boolean isAutoFallback() {
        return autoFallback;
    }

    void setAutoFallback(boolean autoFallback) {
        this.autoFallback = autoFallback;
    }

    boolean isUseParse() {
        return useParse;
    }

    void setUseParse(boolean useParse) {
        this.useParse = useParse;
    }

    String getSearchKeyword() {
        return searchKeyword == null ? "" : searchKeyword;
    }

    void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword == null ? "" : searchKeyword;
    }
}
