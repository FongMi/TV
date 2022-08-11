package com.fongmi.android.tv.bean;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Hot {

    @SerializedName("data")
    private Data data;

    private static Hot objectFrom(String str) {
        return new Gson().fromJson(str, Hot.class);
    }

    public static List<String> get(String str) {
        List<String> items = new ArrayList<>();
        for (Data.Item item : objectFrom(str).getData().getItemList()) items.add(item.getTitle());
        return items;
    }

    private Data getData() {
        return data;
    }

    static class Data {

        @SerializedName("itemList")
        private List<Item> itemList;

        public List<Item> getItemList() {
            return itemList;
        }

        static class Item {

            @SerializedName("title")
            private String title;

            public String getTitle() {
                return title;
            }
        }
    }
}
