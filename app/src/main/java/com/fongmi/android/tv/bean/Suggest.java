package com.fongmi.android.tv.bean;

import com.fongmi.android.tv.App;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Suggest {

    @SerializedName("data")
    private List<Data> data;

    private static Suggest objectFrom(String str) {
        return App.gson().fromJson(str, Suggest.class);
    }

    public static List<String> get(String str) {
        try {
            List<String> items = new ArrayList<>();
            for (Data item : objectFrom(str).getData()) items.add(item.getName());
            return items;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Data> getData() {
        return data;
    }

    static class Data {

        @SerializedName("name")
        private String name;

        private String getName() {
            return name;
        }
    }
}
