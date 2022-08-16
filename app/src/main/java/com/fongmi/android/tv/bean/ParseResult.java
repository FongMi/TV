package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

public class ParseResult {

    @SerializedName("header")
    private JsonElement header;
    @SerializedName("jxFrom")
    private String jxFrom;
    @SerializedName("parse")
    private Integer parse;
    @SerializedName("url")
    private String url;

    public static ParseResult objectFrom(JSONObject object) {
        try {
            return new Gson().fromJson(object.toString(), ParseResult.class);
        } catch (Exception e) {
            return new ParseResult();
        }
    }

    public JsonElement getHeader() {
        return header;
    }

    public String getJxFrom() {
        return TextUtils.isEmpty(jxFrom) ? "" : jxFrom;
    }

    public Integer getParse() {
        return parse == null ? 0 : parse;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public boolean hasHeader() {
        return getHeader() != null;
    }
}
