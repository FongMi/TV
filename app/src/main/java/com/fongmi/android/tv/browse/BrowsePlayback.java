package com.fongmi.android.tv.browse;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;

import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.playback.vod.VodHistoryPolicy;
import com.fongmi.android.tv.setting.Setting;

public final class BrowsePlayback {

    private static final VodHistoryPolicy historyPolicy = new VodHistoryPolicy();

    private final String mediaId;
    private final Result result;
    private final History history;
    private final String channelName;
    private final long resumePositionMs;

    BrowsePlayback(String mediaId, Result result, @Nullable History history, @Nullable String channelName) {
        this.mediaId = mediaId;
        this.result = result;
        this.history = history;
        this.channelName = channelName;
        this.resumePositionMs = history != null && history.getPosition() > 0 ? history.getPosition() : C.TIME_UNSET;
    }

    @Nullable
    public static BrowsePlayback from(MediaItem item) {
        return item.localConfiguration != null && item.localConfiguration.tag instanceof BrowsePlayback playback ? playback : null;
    }

    public static MediaItem withBookmark(MediaItem item, @Nullable History bookmark) {
        BrowsePlayback playback = from(item);
        if (playback == null || playback.history == null || bookmark == null || !playback.history.getKey().equals(bookmark.getKey())) return item;
        History history = playback.history.copy();
        boolean same = history.getVodFlag().equals(bookmark.getVodFlag()) && history.getEpisodeUrl().equals(bookmark.getEpisodeUrl());
        history.setPosition(same ? bookmark.getPosition() : C.TIME_UNSET);
        history.setDuration(same ? bookmark.getDuration() : C.TIME_UNSET);
        return item.buildUpon().setTag(new BrowsePlayback(playback.mediaId, playback.result, history, null)).build();
    }

    @Nullable
    public History copyHistory() {
        return history == null ? null : history.copy();
    }

    public String getMediaId() {
        return mediaId;
    }

    public Result getResult() {
        return result;
    }

    public long getResumePositionMs() {
        return resumePositionMs;
    }

    public boolean matches(String mediaId) {
        return this.mediaId.equals(mediaId);
    }

    @Nullable
    public String getFolderId() {
        return history == null ? null : VodBrowse.VOD_DETAIL + history.getKey();
    }

    @Nullable
    public Boolean isKept() {
        if (history != null) return Keep.find(history.getCid(), history.getKey()) != null;
        return channelName == null ? null : Keep.exist(channelName);
    }

    public boolean toggleKeep() {
        if (history != null) toggleVodKeep();
        else if (channelName != null) toggleLiveKeep();
        else return false;
        return true;
    }

    private void toggleVodKeep() {
        Keep keep = Keep.find(history.getCid(), history.getKey());
        if (keep != null) {
            keep.delete();
        } else {
            keep = new Keep();
            keep.setKey(history.getKey());
            keep.setCid(history.getCid());
            keep.setVodName(history.getVodName());
            keep.setVodPic(history.getVodPic());
            keep.setSiteName(history.getSiteName());
            keep.setCreateTime(System.currentTimeMillis());
            keep.save();
        }
    }

    private void toggleLiveKeep() {
        if (Keep.exist(channelName)) {
            Keep.delete(channelName);
        } else {
            Keep keep = new Keep();
            keep.setKey(channelName);
            keep.setType(1);
            keep.save();
        }
    }

    public void activate(long startPositionMs) {
        if (history == null) return;
        history.setPosition(Math.max(0, startPositionMs));
        history.setCreateTime(System.currentTimeMillis());
        historyPolicy.saveCurrent(history, true);
    }

    public boolean saveProgress(String mediaId, long position, long duration) {
        if (history == null || !matches(mediaId) || position < 0 || duration <= 0 || Setting.isIncognito()) return false;
        history.setPosition(position);
        history.setDuration(duration);
        history.setCreateTime(System.currentTimeMillis());
        historyPolicy.save(history, true);
        return true;
    }
}
