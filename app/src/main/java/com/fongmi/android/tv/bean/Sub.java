package com.fongmi.android.tv.bean;

import android.net.Uri;
import android.text.TextUtils;

import androidx.media3.common.MediaItem;

import com.fongmi.android.tv.player.ExoUtil;
import com.github.catvod.utils.Trans;
import com.google.gson.annotations.SerializedName;

import java.io.File;

public class Sub {

    @SerializedName("url")
    private String url;
    @SerializedName("name")
    private String name;
    @SerializedName("lang")
    private String lang;
    @SerializedName("format")
    private String format;

    public static Sub from(String path) {
        return from(new File(path));
    }

    public static Sub from(File file) {
        Sub sub = new Sub();
        sub.name = file.getName();
        sub.url = file.getAbsolutePath();
        sub.format = ExoUtil.getMimeType(file.getName());
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

    public void trans() {
        if (Trans.pass()) return;
        this.name = Trans.s2t(name);
    }

    public MediaItem.SubtitleConfiguration getExo() {
        return new MediaItem.SubtitleConfiguration.Builder(Uri.parse(getUrl())).setLabel(getName()).setMimeType(getFormat()).setLanguage(getLang()).build();
    }
}
