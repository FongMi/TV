package com.fongmi.android.tv.bean;

import android.net.Uri;
import android.text.TextUtils;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;

import com.fongmi.android.tv.player.ExoUtil;
import com.github.catvod.utils.Path;
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
    @SerializedName("flag")
    private int flag;

    public static Sub from(String path) {
        if (path.startsWith("http")) {
            return http(path);
        } else {
            return file(Path.local(path));
        }
    }

    private static Sub http(String url) {
        Uri uri = Uri.parse(url);
        Sub sub = new Sub();
        sub.url = url;
        sub.name = uri.getLastPathSegment();
        sub.flag = C.SELECTION_FLAG_FORCED;
        sub.format = ExoUtil.getMimeType(uri.getLastPathSegment());
        return sub;
    }

    private static Sub file(File file) {
        Sub sub = new Sub();
        sub.name = file.getName();
        sub.url = file.getAbsolutePath();
        sub.flag = C.SELECTION_FLAG_FORCED;
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

    public int getFlag() {
        return flag == 0 ? C.SELECTION_FLAG_DEFAULT : flag;
    }

    public void trans() {
        if (Trans.pass()) return;
        this.name = Trans.s2t(name);
    }

    public MediaItem.SubtitleConfiguration getExo() {
        return new MediaItem.SubtitleConfiguration.Builder(Uri.parse(getUrl())).setLabel(getName()).setMimeType(getFormat()).setSelectionFlags(getFlag()).setLanguage(getLang()).build();
    }
}
