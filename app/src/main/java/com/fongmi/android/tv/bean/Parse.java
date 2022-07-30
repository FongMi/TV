package com.fongmi.android.tv.bean;

import com.google.gson.annotations.SerializedName;

public class Parse {

    @SerializedName("name")
    private String name;
    @SerializedName("type")
    private Integer type;
    @SerializedName("url")
    private String url;

    public String getName() {
        return name;
    }

    public Integer getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }
}
