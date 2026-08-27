package com.fongmi.android.tv.bean;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.track.TrackUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.utils.Trans;
import com.google.gson.annotations.SerializedName;

public class Sub {

    @SerializedName("url")
    private String url;
    @SerializedName("name")
    private String name;
    @SerializedName("lang")
    private String lang;
    @SerializedName("format")
    private String format;
    @SerializedName("flag")
    private int flag;

    public static Sub from(String url) {
        return from(UrlUtil.path(url), url);
    }

    public static Sub from(String name, String url) {
        Sub sub = new Sub();
        sub.url = url;
        sub.name = name;
        sub.flag = C.SELECTION_FLAG_FORCED;
        sub.format = TrackUtil.getSubtitleMimeType(sub.name);
        return sub;
    }

    public static Sub from(String name, String url, String lang, String format) {
        Sub sub = new Sub();
        sub.name = name;
        sub.url = url;
        sub.lang = lang;
        sub.format = format;
        return sub;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public String getLang() {
        return TextUtils.isEmpty(lang) ? "" : lang;
    }

    public String getFormat() {
        return TextUtils.isEmpty(format) ? "" : format;
    }

    public int getFlag() {
        return flag == 0 ? C.SELECTION_FLAG_DEFAULT : flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public int getRawFlag() {
        return flag;
    }

    public boolean isForced() {
        return (flag & C.SELECTION_FLAG_FORCED) != 0;
    }

    public boolean isEmpty() {
        return url == null || url.isEmpty();
    }

    public Uri getUri() {
        return isEmpty() ? null : UrlUtil.uri(getUrl());
    }

    public void trans() {
        if (Trans.pass()) return;
        this.name = Trans.s2t(name);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Sub it)) return false;
        return getUrl().equals(it.getUrl());
    }

    @NonNull
    @Override
    public String toString() {
        return App.gson().toJson(this);
    }
}
