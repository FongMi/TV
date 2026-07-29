package com.fongmi.android.tv.playback.vod;

import android.text.TextUtils;

import com.fongmi.android.tv.bean.Result;

public record VodDetailResult(String key, String id, Result result) {

    public boolean matches(String key, String id) {
        return TextUtils.equals(this.key, key) && TextUtils.equals(this.id, id);
    }
}
