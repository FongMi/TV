package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.Json;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parse {

    @SerializedName("name")
    private String name;
    @SerializedName("type")
    private Integer type;
    @SerializedName("url")
    private String url;
    @SerializedName("ext")
    private Ext ext;

    private boolean activated;

    public static Parse objectFrom(JsonElement element) {
        return new Gson().fromJson(element, Parse.class);
    }

    public static Parse get(String name) {
        Parse parse = new Parse();
        parse.setName(name);
        return parse;
    }

    public static Parse get(Integer type, String url) {
        Parse parse = new Parse();
        parse.setType(type);
        parse.setUrl(url);
        return parse;
    }

    public static Parse get(Integer type, String url, JsonElement header) {
        Parse parse = new Parse();
        parse.setHeader(header);
        parse.setType(type);
        parse.setUrl(url);
        return parse;
    }

    public static Parse god() {
        Parse parse = new Parse();
        parse.setName(ResUtil.getString(R.string.parse_god));
        parse.setType(4);
        return parse;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type == null ? 0 : type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : Utils.checkProxy(url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Ext getExt() {
        return ext = ext == null ? new Ext() : ext;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public void setActivated(Parse item) {
        this.activated = item.equals(this);
    }

    private void setHeader(JsonElement header) {
        getExt().setHeader(header);
    }

    public Map<String, String> getHeaders() {
        return Json.toMap(getExt().getHeader());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Parse)) return false;
        Parse it = (Parse) obj;
        return getName().equals(it.getName());
    }

    public String extUrl() {
        int index = getUrl().indexOf("?");
        if (getExt().isEmpty() || index == -1) return getUrl();
        return getUrl().substring(0, index + 1) + "cat_ext=" + Utils.getBase64(getExt().toString()) + "&" + getUrl().substring(index + 1);
    }

    public HashMap<String, String> mixMap() {
        HashMap<String, String> map = new HashMap<>();
        map.put("type", getType().toString());
        map.put("ext", getExt().toString());
        map.put("url", getUrl());
        return map;
    }

    public static class Ext {

        @SerializedName("flag")
        private List<String> flag;
        @SerializedName("header")
        private JsonElement header;

        public List<String> getFlag() {
            return flag == null ? Collections.emptyList() : flag;
        }

        public JsonElement getHeader() {
            return header;
        }

        public void setHeader(JsonElement header) {
            this.header = header;
        }

        public boolean isEmpty() {
            return flag == null && header == null;
        }

        @NonNull
        @Override
        public String toString() {
            return new Gson().toJson(this);
        }
    }
}
