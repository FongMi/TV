package com.hiker.drpy.bean;

import android.text.TextUtils;

import com.github.catvod.utils.Json;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Res {

    @SerializedName("code")
    private Integer code;
    @SerializedName("buffer")
    private Integer buffer;
    @SerializedName("content")
    private String content;
    @SerializedName("headers")
    private JsonElement headers;

    public static Res objectFrom(String json) {
        return new Gson().fromJson(json, Res.class);
    }

    public int getCode() {
        return code == null ? 200 : code;
    }

    public int getBuffer() {
        return buffer == null ? 0 : buffer;
    }

    public String getContent() {
        return TextUtils.isEmpty(content) ? "" : content;
    }

    private JsonElement getHeaders() {
        return headers;
    }

    public Map<String, String> getHeader() {
        return Json.toMap(getHeaders());
    }
}
