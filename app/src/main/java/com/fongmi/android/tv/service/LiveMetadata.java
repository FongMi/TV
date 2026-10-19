package com.fongmi.android.tv.service;

import android.util.Log;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.browse.BrowseTree;
import com.fongmi.android.tv.player.PlayerManager;
import com.github.catvod.utils.Task;

import java.util.concurrent.Future;

final class LiveMetadata {

    private final PlayerManager player;
    private final BrowseLibrary library;
    private final Runnable refreshTask = this::refresh;

    private Future<?> request;
    private long generation;

    LiveMetadata(PlayerManager player, BrowseLibrary library) {
        this.player = player;
        this.library = library;
    }

    void refresh() {
        cancel();
        MediaItem item = player.getCurrentMediaItem();
        if (item == null || !item.mediaId.startsWith("LC:")) return;
        long currentGeneration = generation;
        request = Task.submit(() -> {
            MediaMetadata metadata = loadMetadata(item.mediaId);
            App.post(() -> onLoaded(item, currentGeneration, metadata));
        });
    }

    private MediaMetadata loadMetadata(String mediaId) {
        try {
            return BrowseTree.loadLiveMetadata(mediaId);
        } catch (Exception error) {
            Log.w("LiveMetadata", "Unable to load live programme information", error);
            return null;
        }
    }

    private void onLoaded(MediaItem item, long currentGeneration, MediaMetadata metadata) {
        if (currentGeneration != generation) return;
        MediaItem current = player.getCurrentMediaItem();
        if (current == null || !item.mediaId.equals(current.mediaId)) return;
        if (metadata != null) player.setMetadata(metadata);
        String parent = BrowseTree.getLiveGroupId(item.mediaId);
        if (parent != null) library.notifyChildrenChanged(parent);
        library.notifyChildrenChanged(BrowseTree.LIVE_KEEP);
        App.post(refreshTask, 30_000);
    }

    void cancel() {
        generation++;
        App.removeCallbacks(refreshTask);
        if (request != null) request.cancel(true);
        request = null;
    }
}
