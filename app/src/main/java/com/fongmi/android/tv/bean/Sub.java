package com.fongmi.android.tv.bean;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;

import com.fongmi.android.tv.player.exo.ExoUtil;
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

    public static Sub from(String path) {
        Uri uri = Uri.parse(path);
        Sub sub = new Sub();
        sub.url = path;
        sub.name = uri.getLastPathSegment();
        sub.flag = C.SELECTION_FLAG_FORCED;
        sub.format = ExoUtil.getMimeType(sub.name);
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

    public void trans() {
        if (Trans.pass()) return;
        this.name = Trans.s2t(name);
    }

    public MediaItem.SubtitleConfiguration config() {
        return new MediaItem.SubtitleConfiguration.Builder(Uri.parse(UrlUtil.convert(getUrl()))).setLabel(getName()).setMimeType(getFormat()).setSelectionFlags(getFlag()).setLanguage(getLang()).build();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Sub)) return false;
        Sub it = (Sub) obj;
        return getUrl().equals(it.getUrl());
    }
}
