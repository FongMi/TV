package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.db.AppDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

@Entity(ignoredColumns = {"type", "api", "playUrl", "ext", "categories", "jar"})
public class Site {

    @NonNull
    @PrimaryKey
    @SerializedName("key")
    private String key;
    @SerializedName("name")
    private String name;
    @SerializedName("type")
    private int type;
    @SerializedName("api")
    private String api;
    @SerializedName("playUrl")
    private String playUrl;
    @SerializedName("searchable")
    private Integer searchable;
    @SerializedName("filterable")
    private Integer filterable;
    @SerializedName("ext")
    private String ext;
    @SerializedName("jar")
    private String jar;
    @SerializedName("categories")
    private List<String> categories;

    private boolean activated;

    public static Site objectFrom(JsonElement element) {
        try {
            return new Gson().fromJson(element, Site.class);
        } catch (Exception e) {
            return new Site();
        }
    }

    public static Site get(String key) {
        Site site = new Site();
        site.setKey(key);
        return site;
    }

    public static Site get(String key, String name) {
        Site site = new Site();
        site.setKey(key);
        site.setName(name);
        return site;
    }

    public String getKey() {
        return TextUtils.isEmpty(key) ? "" : key;
    }

    public void setKey(@NonNull String key) {
        this.key = key;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getApi() {
        return TextUtils.isEmpty(api) ? "" : api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getPlayUrl() {
        return playUrl;
    }

    public Integer getSearchable() {
        return searchable;
    }

    public void setSearchable(Integer searchable) {
        this.searchable = searchable;
    }

    public Integer getFilterable() {
        return filterable;
    }

    public void setFilterable(Integer filterable) {
        this.filterable = filterable;
    }

    public String getExt() {
        return TextUtils.isEmpty(ext) ? "" : ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getJar() {
        return TextUtils.isEmpty(jar) ? "" : jar;
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

    public boolean isSearchable() {
        return getSearchable() == null || getSearchable() == 1;
    }

    public Site setSearchable(boolean searchable) {
        setSearchable(searchable ? 1 : 0);
        return this;
    }

    public boolean isFilterable() {
        return getFilterable() == null || getFilterable() == 1;
    }

    public Site setFilterable(boolean filterable) {
        setFilterable(filterable ? 1 : 0);
        return this;
    }

    public int getSearchIcon() {
        return isSearchable() ? R.drawable.ic_search_on : R.drawable.ic_search_off;
    }

    public int getFilterIcon() {
        return isFilterable() ? R.drawable.ic_filter_on : R.drawable.ic_filter_off;
    }

    public static Site find(String key) {
        return AppDatabase.get().getSiteDao().find(key);
    }

    public void save() {
        AppDatabase.get().getSiteDao().insertOrUpdate(this);
    }

    public Site sync() {
        Site item = find(getKey());
        if (item == null) return this;
        setSearchable(item.getSearchable());
        setFilterable(item.getFilterable());
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Site)) return false;
        Site it = (Site) obj;
        return getKey().equals(it.getKey());
    }
}
