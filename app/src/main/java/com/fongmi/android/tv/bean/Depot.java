package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.fongmi.android.tv.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class Depot {

    @SerializedName("url")
    private String url;
    @SerializedName("name")
    private String name;

    public static List<Depot> arrayFrom(String str) {
        Type listType = new TypeToken<List<Depot>>() {}.getType();
        List<Depot> items = new Gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : Utils.checkClan(url);
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? getUrl() : name;
    }
}
