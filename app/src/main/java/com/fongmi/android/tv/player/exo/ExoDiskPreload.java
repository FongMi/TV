package com.fongmi.android.tv.player.exo;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PriorityTaskManager;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.preload.DiskPreloadManager;

import com.fongmi.android.tv.player.media.ConcatenatingUrl;
import com.fongmi.android.tv.setting.PreloadSetting;
import com.github.catvod.utils.Auth;

import java.util.Map;

public class ExoDiskPreload {

    private final PriorityTaskManager priorityTaskManager;
    private DiskPreloadManager manager;
    private Map<String, String> headers;
    private Auth.Context auth;
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
        Auth.Context auth = ExoUtil.extractAuth(mediaItem);
        if (manager == null || !headers.equals(this.headers) || auth != this.auth) {
            releaseManager();
            manager = createManager(headers, auth);
            this.headers = headers;
            this.auth = auth;
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
        auth = null;
    }

    private DiskPreloadManager createManager(Map<String, String> headers, Auth.Context auth) {
        return new DiskPreloadManager.Builder(ExoMediaSourceFactory.getCache(), ExoMediaSourceFactory.createUpstreamDataSourceFactory(headers, auth), ExoUtil.buildRenderersFactory()).setPriorityTaskManager(priorityTaskManager).build();
    }

    private DiskPreloadManager.Options createOptions() {
        return DiskPreloadManager.Options.builder().setDurationMs(PreloadSetting.getDurationMs()).setMaxThreads(PreloadSetting.getThreads()).build();
    }
}
