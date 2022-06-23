package com.fongmi.bear.bean;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Site {

    @SerializedName("key")
    private String key;
    @SerializedName("name")
    private String name;
    @SerializedName("type")
    private Integer type;
    @SerializedName("api")
    private String api;
    @SerializedName("searchable")
    private Integer searchable;
    @SerializedName("quickSearch")
    private Integer quickSearch;
    @SerializedName("filterable")
    private Integer filterable;
    @SerializedName("ext")
    private String ext;

    public static List<Site> arrayFrom(String str) {
        Type listType = new TypeToken<ArrayList<Site>>() {}.getType();
        return new Gson().fromJson(str, listType);
    }

    public String getKey() {
        return key;
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
}
