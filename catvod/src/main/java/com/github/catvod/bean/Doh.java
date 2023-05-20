package com.github.catvod.bean;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class Doh {

    @SerializedName("name")
    private String name;
    @SerializedName("url")
    private String url;
    @SerializedName("ips")
    private List<String> ips;

    public static Doh objectFrom(String str) {
        return new Gson().fromJson(str, Doh.class);
    }

    public static List<Doh> arrayFrom(JsonElement element) {
        Type listType = new TypeToken<List<Doh>>() {}.getType();
        List<Doh> items = new Gson().fromJson(element, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public Doh(String name) {
        this.name = name;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public List<String> getIps() {
        return ips == null ? Collections.emptyList() : ips;
    }
}
