package com.fongmi.android.tv.playback.live;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.Group;

public class LivePlaybackState {

    private LivePlayRequest pendingRequest;
    private LivePlayRequest playingRequest;
    private MediaMetadata playbackMetadata;
    private String playbackKey;
    private Channel channel;
    private Group group;

    public void reset() {
        clearPlayback();
        channel = null;
        group = null;
    }

    void clearPlayback() {
        pendingRequest = null;
        playingRequest = null;
        playbackMetadata = null;
        playbackKey = null;
    }

    Group getGroup() {
        return group;
    }

    void setGroup(Group group) {
        this.group = group;
    }

    Channel getChannel() {
        return channel;
    }

    void setChannel(Channel channel) {
        this.channel = channel;
        this.group = channel != null ? channel.getGroup() : group;
    }

    @Nullable
    LivePlayRequest getPendingRequest() {
        return pendingRequest;
    }

    @Nullable
    LivePlayRequest getActiveRequest() {
        return pendingRequest != null ? pendingRequest : playingRequest;
    }

    @Nullable
    LivePlayRequest getPlayingRequest() {
        return playingRequest;
    }

    void setPendingRequest(LivePlayRequest pendingRequest) {
        this.pendingRequest = pendingRequest;
        this.playingRequest = null;
        this.playbackKey = null;
    }

    void setPlayingRequest(LivePlayRequest playingRequest, String playbackKey) {
        this.playingRequest = playingRequest;
        this.playbackKey = playbackKey;
        this.pendingRequest = null;
    }

    @Nullable
    String getPlaybackKey() {
        return playbackKey;
    }

    @Nullable
    MediaMetadata getPlaybackMetadata() {
        return playbackMetadata;
    }

    void setPlaybackMetadata(MediaMetadata playbackMetadata) {
        this.playbackMetadata = playbackMetadata;
    }

    void clearPendingRequest() {
        this.pendingRequest = null;
    }
}
