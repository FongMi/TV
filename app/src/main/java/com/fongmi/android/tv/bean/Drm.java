package com.fongmi.android.tv.bean;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;

import com.google.gson.annotations.SerializedName;

import java.nio.charset.StandardCharsets;

public class Drm {

    @SerializedName("licenseType")
    private String licenseType;
    @SerializedName("licenseKey")
    private String licenseKey;

    public Drm(String licenseType, String licenseKey) {
        this.licenseType = licenseType;
        this.licenseKey = licenseKey;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public MediaItem.DrmConfiguration get() {
        return new MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID).setKeySetId(getLicenseKey().getBytes(StandardCharsets.UTF_8)).build();
    }
}
