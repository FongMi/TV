package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Part {

    @SerializedName("t")
    private String t;

    private static List<Part> arrayFrom(String str) {
        Type listType = new TypeToken<List<Part>>() {}.getType();
        List<Part> items = new Gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public static List<String> get(String str) {
        List<String> items = new ArrayList<>();
        if (TextUtils.isEmpty(str)) return items;
        for (Part item : arrayFrom(str)) items.add(item.getT());
        return items;
    }

    public String getT() {
        return TextUtils.isEmpty(t) ? "" : t;
    }
}
