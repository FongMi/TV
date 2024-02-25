package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Part {

    @SerializedName("data")
    private Data data;

    public static Part objectFrom(String str) {
        return App.gson().fromJson(str, Part.class);
    }

    public static List<String> get(String str) {
        List<String> items = new ArrayList<>();
        if (TextUtils.isEmpty(str)) return items;
        for (Data.Word word : objectFrom(str).getData().getWords()) items.add(word.getWord());
        return items;
    }

    public Data getData() {
        return data == null ? new Data() : data;
    }

    public static class Data {

        @SerializedName("words")
        private List<Word> words;

        public List<Word> getWords() {
            return words == null ? Collections.emptyList() : words;
        }

        public static class Word {

            @SerializedName("word")
            private String word;

            public String getWord() {
                return TextUtils.isEmpty(word) ? "" : word;
            }
        }
    }
}
