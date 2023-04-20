package com.fongmi.android.tv.bean;

import android.net.Uri;
import android.text.TextUtils;

import androidx.media3.common.MediaItem;

import com.fongmi.android.tv.utils.Trans;
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

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public String getLang() {
        return TextUtils.isEmpty(lang) ? "zh" : lang;
    }

    public String getFormat() {
        return TextUtils.isEmpty(format) ? "" : format;
    }

    public void trans() {
        if (Trans.pass()) return;
        this.name = Trans.s2t(name);
    }

    public MediaItem.SubtitleConfiguration getExo() {
        return new MediaItem.SubtitleConfiguration.Builder(Uri.parse(getUrl())).setLabel(getName()).setMimeType(getFormat()).setLanguage(getLang()).build();
    }
}
