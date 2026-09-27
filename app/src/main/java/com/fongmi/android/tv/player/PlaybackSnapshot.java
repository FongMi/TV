package com.fongmi.android.tv.player;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;

import com.github.catvod.crawler.SpiderDebug;

public record PlaybackSnapshot(boolean released, String url, String key, MediaMetadata metadata, MediaMetadata playingMetadata,
                               long position, long duration, float speed, int state) {

    public static PlaybackSnapshot empty() {
        return new PlaybackSnapshot(true, "", "", MediaMetadata.EMPTY, MediaMetadata.EMPTY, 0, 0, 1, 1);
    }

    public static PlaybackSnapshot pending(PlayerManager player) {
        if (player == null) return empty();
        String url = text(player.getUrl());
        String key = text(player.getKey());
        MediaMetadata metadata = player.getMetadata();
        if (metadata == null) metadata = MediaMetadata.EMPTY;
        return new PlaybackSnapshot(true, url, key, metadata, MediaMetadata.EMPTY, 0, 0, 1, 1);
    }

    public static PlaybackSnapshot capture(PlayerManager player) {
        PlaybackSnapshot pending = pending(player);
        if (player == null || player.isReleased()) return pending;
        MediaItem item = player.getCurrentMediaItem();
        MediaMetadata playingMetadata = item == null ? MediaMetadata.EMPTY : item.mediaMetadata;
        long position = 0;
        long duration = 0;
        try {
            position = player.getPosition();
            duration = player.getDuration();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return new PlaybackSnapshot(false, pending.url(), pending.key(), pending.metadata(), playingMetadata,
                position, duration, player.getSpeed(), state(player));
    }

    private static int state(PlayerManager player) {
        if (player.isPlaying()) return 3;
        int state = player.getPlaybackState();
        if (state == Player.STATE_BUFFERING) return 6;
        if (state == Player.STATE_READY) return 2;
        return 1;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
