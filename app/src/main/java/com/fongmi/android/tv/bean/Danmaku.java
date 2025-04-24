package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class Danmaku {

    @SerializedName("name")
    private String name;
    @SerializedName("url")
    private String url;

    private boolean selected;

    public static Danmaku from(String path) {
        Danmaku danmaku = new Danmaku();
        danmaku.setName(path);
        danmaku.setUrl(path);
        return danmaku;
    }

    public static Danmaku empty() {
        return new Danmaku();
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? getUrl() : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isEmpty() {
        return getUrl().isEmpty();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Danmaku)) return false;
        Danmaku it = (Danmaku) obj;
        return getUrl().equals(it.getUrl());
    }
}