package com.fongmi.android.tv.playback.vod;

import android.text.TextUtils;

import androidx.media3.common.C;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.Result;

public final class VodPlayRequest {

    private final String flag;
    private final String key;
    private final String id;
    private final boolean builtin;
    private final int qualityPosition;
    private final long startPositionMs;

    private VodPlayRequest(String key, String flag, String id, boolean builtin, int qualityPosition, long startPositionMs) {
        this.flag = flag == null ? "" : flag;
        this.key = key == null ? "" : key;
        this.id = id == null ? "" : id;
        this.builtin = builtin;
        this.qualityPosition = qualityPosition;
        this.startPositionMs = startPositionMs;
    }

    public static VodPlayRequest create(String key, Flag flag, Episode episode) {
        return create(key, flag, episode, false);
    }

    public static VodPlayRequest create(String key, Flag flag, Episode episode, boolean builtin) {
        return create(key, flag, episode, builtin, 0, C.TIME_UNSET);
    }

    public static VodPlayRequest create(String key, Flag flag, Episode episode, boolean builtin, int qualityPosition, long startPositionMs) {
        return new VodPlayRequest(key, flag.getFlag(), episode.getUrl(), builtin, qualityPosition, startPositionMs);
    }

    public boolean isBuiltin() {
        return builtin;
    }

    public String getFlag() {
        return flag;
    }

    public String getKey() {
        return key;
    }

    public String getId() {
        return id;
    }

    public int getQualityPosition() {
        return qualityPosition;
    }

    public long getStartPositionMs() {
        return startPositionMs;
    }

    public boolean matches(String key, Flag flag, Episode episode) {
        return TextUtils.equals(this.key, key) && flag != null && episode != null && TextUtils.equals(this.flag, flag.getFlag()) && TextUtils.equals(this.id, episode.getUrl());
    }

    public boolean matches(VodPlayRequest request) {
        return request != null && builtin == request.builtin && qualityPosition == request.qualityPosition && TextUtils.equals(key, request.key) && TextUtils.equals(flag, request.flag) && TextUtils.equals(id, request.id);
    }

    public boolean accepts(Result result) {
        return result != null && (result.getFlag().isEmpty() || TextUtils.equals(flag, result.getFlag()));
    }
}
