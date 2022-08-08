package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class Site {

    @SerializedName("key")
    private String key;
    @SerializedName("name")
    private String name;
    @SerializedName("type")
    private int type;
    @SerializedName("api")
    private String api;
    @SerializedName("playerUrl")
    private String playerUrl;
    @SerializedName("searchable")
    private Integer searchable;
    @SerializedName("quickSearch")
    private Integer quickSearch;
    @SerializedName("filterable")
    private Integer filterable;
    @SerializedName("ext")
    private String ext;
    @SerializedName("categories")
    private List<String> categories;

    private boolean home;

    public static Site objectFrom(JsonElement element) {
        return new Gson().fromJson(element, Site.class);
    }

    public static Site get(String key) {
        Site site = new Site();
        site.setKey(key);
        return site;
    }

    public String getKey() {
        return TextUtils.isEmpty(key) ? "" : key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public int getType() {
        return type;
    }

    public String getApi() {
        return TextUtils.isEmpty(api) ? "" : api;
    }

    public String getPlayerUrl() {
        return TextUtils.isEmpty(playerUrl) ? "" : playerUrl;
    }

    public boolean isSearchable() {
        return searchable == null || searchable == 1;
    }

    public boolean isQuickSearch() {
        return quickSearch == null || quickSearch == 1;
    }

    public boolean isFilterable() {
        return filterable == null || filterable == 1;
    }

    public String getExt() {
        return TextUtils.isEmpty(ext) ? "" : ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public List<String> getCategories() {
        return categories == null ? Collections.emptyList() : categories;
    }

    public boolean isHome() {
        return home;
    }

    public void setHome(boolean home) {
        this.home = home;
    }

    public void setHome(Site item) {
        this.home = item.equals(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Site)) return false;
        Site it = (Site) obj;
        return getKey().equals(it.getKey());
    }
}
