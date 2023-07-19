package com.fongmi.quickjs.bean;

import android.text.TextUtils;

import com.github.catvod.utils.Json;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Req {

    @SerializedName("buffer")
    private Integer buffer;
    @SerializedName("redirect")
    private Integer redirect;
    @SerializedName("timeout")
    private Integer timeout;
    @SerializedName("method")
    private String method;
    @SerializedName("body")
    private String body;
    @SerializedName("data")
    private String data;
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

    public String getMethod() {
        return TextUtils.isEmpty(method) ? "get" : method;
    }

    public String getBody() {
        return TextUtils.isEmpty(body) ? "" : body;
    }

    public String getData() {
        return TextUtils.isEmpty(data) ? "" : data;
    }

    private JsonElement getHeaders() {
        return headers;
    }

    public Map<String, String> getHeader() {
        return Json.toMap(getHeaders());
    }
}
