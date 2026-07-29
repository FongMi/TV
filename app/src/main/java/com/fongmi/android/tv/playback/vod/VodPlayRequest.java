package com.fongmi.android.tv.playback.vod;

import android.text.TextUtils;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.Result;

public final class VodPlayRequest {

    private final String flag;
    private final String key;
    private final String id;

    private VodPlayRequest(String key, String flag, String id) {
        this.flag = flag == null ? "" : flag;
        this.key = key == null ? "" : key;
        this.id = id == null ? "" : id;
    }

    public static VodPlayRequest create(String key, Flag flag, Episode episode) {
        return new VodPlayRequest(key, flag.getFlag(), episode.getUrl());
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

    public boolean matches(String key, Flag flag, Episode episode) {
        return TextUtils.equals(this.key, key) && flag != null && episode != null && TextUtils.equals(this.flag, flag.getFlag()) && TextUtils.equals(this.id, episode.getUrl());
    }

    public boolean matches(VodPlayRequest request) {
        return request != null && TextUtils.equals(key, request.key) && TextUtils.equals(flag, request.flag) && TextUtils.equals(id, request.id);
    }

    public boolean accepts(Result result) {
        return result != null && (result.getFlag().isEmpty() || TextUtils.equals(flag, result.getFlag()));
    }
}
