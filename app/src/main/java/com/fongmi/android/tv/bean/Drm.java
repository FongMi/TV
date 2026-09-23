package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.media3.common.C;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.gson.HeaderAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class Drm {

    @SerializedName("key")
    private String key;
    @SerializedName("type")
    private String type;
    @SerializedName("forceKey")
    private boolean forceKey;
    @SerializedName("header")
    @JsonAdapter(HeaderAdapter.class)
    private Map<String, String> header;

    private Drm(String key, String type, Map<String, String> header, boolean forceKey) {
        this.key = key;
        this.type = type;
        this.header = header;
        this.forceKey = forceKey;
    }

    public static Drm create(String key, String type, Map<String, String> header, boolean forceKey) {
        return new Drm(key, type, header, forceKey);
    }

    public String getKey() {
        return TextUtils.isEmpty(key) ? "" : key;
    }

    public String getType() {
        return TextUtils.isEmpty(type) ? "" : type;
    }

    public boolean isForceKey() {
        return forceKey;
    }

    public Map<String, String> getHeader() {
        return header == null ? new HashMap<>() : header;
    }

    public UUID getUUID() {
        String type = getType().toLowerCase(Locale.ROOT);
        if (type.contains("playready")) return C.PLAYREADY_UUID;
        if (type.contains("widevine")) return C.WIDEVINE_UUID;
        if (type.contains("clearkey")) return C.CLEARKEY_UUID;
        return C.UUID_NIL;
    }

    @NonNull
    @Override
    public String toString() {
        return App.gson().toJson(this);
    }
}
