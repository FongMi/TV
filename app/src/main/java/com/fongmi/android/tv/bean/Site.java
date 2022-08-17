package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.fongmi.android.tv.R;
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
    @SerializedName("filterable")
    private Integer filterable;
    @SerializedName("ext")
    private String ext;
    @SerializedName("categories")
    private List<String> categories;

    private boolean activated;

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

    public void setSearchable(boolean searchable) {
        this.searchable = searchable ? 1 : 0;
    }

    public boolean isFilterable() {
        return filterable == null || filterable == 1;
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable ? 1 : 0;
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

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public void setActivated(Site item) {
        this.activated = item.equals(this);
    }

    public String getActivatedName() {
        return (isActivated() ? "√ " : "").concat(getName());
    }

    public int getSearchIcon() {
        return isSearchable() ? R.drawable.ic_search_on : R.drawable.ic_search_off;
    }

    public int getFilterIcon() {
        return isFilterable() ? R.drawable.ic_filter_on : R.drawable.ic_filter_off;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Site)) return false;
        Site it = (Site) obj;
        return getKey().equals(it.getKey());
    }
}
