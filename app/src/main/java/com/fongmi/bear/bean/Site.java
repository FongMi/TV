package com.fongmi.bear.bean;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

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
    private int searchable;
    @SerializedName("quickSearch")
    private int quickSearch;
    @SerializedName("filterable")
    private int filterable;
    @SerializedName("ext")
    private String ext;

    private boolean home;

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
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getPlayerUrl() {
        return playerUrl;
    }

    public Integer getSearchable() {
        return searchable;
    }

    public void setSearchable(Integer searchable) {
        this.searchable = searchable;
    }

    public Integer getQuickSearch() {
        return quickSearch;
    }

    public void setQuickSearch(Integer quickSearch) {
        this.quickSearch = quickSearch;
    }

    public Integer getFilterable() {
        return filterable;
    }

    public void setFilterable(Integer filterable) {
        this.filterable = filterable;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public boolean isHome() {
        return home;
    }

    public void setHome(boolean home) {
        this.home = home;
    }

    public void setActivated(Site item) {
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
