package com.fongmi.android.tv.bean;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class Rule {

    @SerializedName("hosts")
    private List<String> hosts;
    @SerializedName("regex")
    private List<String> regex;

    public static List<Rule> arrayFrom(JsonElement element) {
        Type listType = new TypeToken<List<Rule>>() {}.getType();
        List<Rule> items = new Gson().fromJson(element, listType);
        return items == null ? Collections.emptyList() : items;
    }

    public List<String> getHosts() {
        return hosts == null ? Collections.emptyList() : hosts;
    }

    public List<String> getRegex() {
        return regex == null ? Collections.emptyList() : regex;
    }
}
