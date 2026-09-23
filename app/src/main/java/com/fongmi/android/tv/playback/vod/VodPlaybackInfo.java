package com.fongmi.android.tv.playback.vod;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.History;

import java.util.Objects;

public final class VodPlaybackInfo {

    private static volatile Snapshot snapshot = new Snapshot("", "", "", "");

    private VodPlaybackInfo() {
    }

    public static Snapshot get() {
        return snapshot;
    }

    public static void update(History history, Flag flag, Episode episode) {
        if (history == null || flag == null || episode == null) return;
        snapshot = new Snapshot(Objects.toString(history.getVodName(), ""), episode.getName(), flag.getFlag(), episode.getUrl());
    }

    public record Snapshot(String title, String episodeName, String flag, String id) {}
}
