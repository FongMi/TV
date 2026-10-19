package com.fongmi.android.tv.player.exo;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PriorityTaskManager;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.preload.DiskPreloadManager;

import com.fongmi.android.tv.player.media.ConcatenatingUrl;
import com.fongmi.android.tv.setting.PreloadSetting;

import java.util.Map;

public class ExoDiskPreload {

    private final PriorityTaskManager priorityTaskManager;
    private DiskPreloadManager manager;
    private Map<String, String> headers;
    private ExoPlayer player;

    public ExoDiskPreload() {
        this.priorityTaskManager = new PriorityTaskManager();
    }

    public void start(ExoPlayer player, MediaItem mediaItem) {
        stop();
        if (!PreloadSetting.isEnabled() || ConcatenatingUrl.matches(mediaItem.localConfiguration.uri.toString())) {
            releaseManager();
            return;
        }
        Map<String, String> headers = ExoUtil.extractHeaders(mediaItem);
        if (manager == null || !headers.equals(this.headers)) {
            releaseManager();
            manager = createManager(headers);
            this.headers = headers;
        }
        this.player = player;
        player.setPriorityTaskManager(priorityTaskManager);
        manager.start(player, mediaItem, createOptions());
    }

    public void stop() {
        if (manager != null) manager.stop();
        if (player != null) player.setPriorityTaskManager(null);
        player = null;
    }

    public void release() {
        stop();
        releaseManager();
    }

    private void releaseManager() {
        if (manager == null) return;
        manager.release();
        manager = null;
        headers = null;
    }

    private DiskPreloadManager createManager(Map<String, String> headers) {
        return new DiskPreloadManager.Builder(ExoMediaSourceFactory.getCache(), ExoMediaSourceFactory.createUpstreamDataSourceFactory(headers), ExoUtil.buildRenderersFactory()).setPriorityTaskManager(priorityTaskManager).build();
    }

    private DiskPreloadManager.Options createOptions() {
        return DiskPreloadManager.Options.builder().setDurationMs(PreloadSetting.getDurationMs()).setMaxThreads(PreloadSetting.getThreads()).build();
    }
}
