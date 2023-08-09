package com.fongmi.quickjs.bean;

import android.text.TextUtils;
import android.util.Base64;

import com.github.catvod.utils.Json;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public String getContentType() {
        Map<String, String> header = getHeader();
        List<String> keys = Arrays.asList("Content-Type", "content-type");
        for (String key : keys) if (header.containsKey(key)) return Objects.requireNonNull(header.get(key));
        return "application/octet-stream";
    }

    public ByteArrayInputStream getStream() {
        if (getBuffer() == 2) return new ByteArrayInputStream(Base64.decode(getContent(), Base64.DEFAULT));
        return new ByteArrayInputStream(getContent().getBytes());
    }
}
