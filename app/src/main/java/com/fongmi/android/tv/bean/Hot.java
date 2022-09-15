package com.fongmi.android.tv.bean;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hot {

    @SerializedName("data")
    private List<Data> data;

    private static Hot objectFrom(String str) {
        return new Gson().fromJson(str, Hot.class);
    }

    public static List<String> get(String str) {
        try {
            List<String> items = new ArrayList<>();
            for (Data item : objectFrom(str).getData()) items.add(item.getTitle());
            return items;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public List<Data> getData() {
        return data;
    }

    public static class Data {

        @SerializedName("title")
        private String title;

        public String getTitle() {
            return title;
        }
    }
}
