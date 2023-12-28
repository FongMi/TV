package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;

import com.fongmi.android.tv.server.Server;
import com.github.catvod.utils.Util;
import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class Drm {

    @SerializedName("key")
    private String key;
    @SerializedName("type")
    private String type;

    public static Drm create(String key, String type) {
        return new Drm(key, type);
    }

    private Drm(String key, String type) {
        this.key = key;
        this.type = type;
    }

    private String getKey() {
        return TextUtils.isEmpty(key) ? "" : key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    private String getType() {
        return TextUtils.isEmpty(type) ? "" : type;
    }

    private UUID getUUID() {
        if (getType().contains("widevine")) return C.WIDEVINE_UUID;
        if (getType().contains("clearkey")) return C.CLEARKEY_UUID;
        return C.UUID_NIL;
    }

    private String getUri() {
        if (getKey().startsWith("http")) return getKey();
        if (!getKey().startsWith("{") && getKey().contains(":")) convert();
        return Server.get().getAddress("license/") + Util.base64(getKey());
    }

    private void convert() {
        String[] split = getKey().split(":");
        String kid = getBase64(split[0]);
        String key = getBase64(split[1]);
        setKey(String.format("{ \"keys\":[ { \"kty\":\"oct\", \"k\":\"%s\", \"kid\":\"%s\" } ], \"type\":\"temporary\" }", key, kid));
    }

    private String getBase64(String s) {
        return Util.base64(Util.hex2byte(s)).replace("=", "");
    }

    public MediaItem.DrmConfiguration get() {
        return new MediaItem.DrmConfiguration.Builder(getUUID()).setLicenseUri(getUri()).build();
    }
}
