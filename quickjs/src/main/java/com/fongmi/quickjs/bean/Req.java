package com.fongmi.quickjs.bean;

import android.text.TextUtils;

import com.github.catvod.utils.Json;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Req {

    @SerializedName("buffer")
    private Integer buffer;
    @SerializedName("redirect")
    private Integer redirect;
    @SerializedName("timeout")
    private Integer timeout;
    @SerializedName("postType")
    private String postType;
    @SerializedName("method")
    private String method;
    @SerializedName("body")
    private String body;
    @SerializedName("data")
    private JsonElement data;
    @SerializedName("headers")
    private JsonElement headers;

    public static Req objectFrom(String json) {
        return new Gson().fromJson(json, Req.class);
    }

    public int getBuffer() {
        return buffer == null ? 0 : buffer;
    }

    public Integer getRedirect() {
        return redirect == null ? 1 : redirect;
    }

    public Integer getTimeout() {
        return timeout == null ? 10000 : timeout;
    }

    public String getPostType() {
        return TextUtils.isEmpty(postType) ? "json" : postType;
    }

    public String getMethod() {
        return TextUtils.isEmpty(method) ? "get" : method;
    }

    public String getBody() {
        return body;
    }

    public JsonElement getData() {
        return data;
    }

    private JsonElement getHeaders() {
        return headers;
    }

    public Map<String, String> getHeader() {
        return Json.toMap(getHeaders());
    }

    public String getCharset() {
        Map<String, String> header = getHeader();
        List<String> keys = Arrays.asList("Content-Type", "content-type");
        for (String key : keys) if (header.containsKey(key)) return getCharset(Objects.requireNonNull(header.get(key)));
        return "UTF-8";
    }

    private String getCharset(String value) {
        for (String text : value.split(";")) if (text.contains("charset=")) return text.split("=")[1];
        return "UTF-8";
    }
}
