package com.fongmi.bear.bean;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class Result {

    @SerializedName("class")
    private List<Class> types;
    @SerializedName("list")
    private List<Vod> list;
    @SerializedName("filters")
    private LinkedHashMap<String, List<Filter>> filters;

    public static Result objectFrom(String str) {
        try {
            Type type = new TypeToken<LinkedHashMap<String, List<Filter>>>() {}.getType();
            Gson gson = new GsonBuilder().registerTypeAdapter(type, new FiltersAdapter()).create();
            Result result = gson.fromJson(str, Result.class);
            return result == null ? new Result() : result;
        } catch (Exception e) {
            e.printStackTrace();
            return new Result();
        }
    }

    public List<Class> getTypes() {
        return types == null ? Collections.emptyList() : types;
    }

    public List<Vod> getList() {
        return list == null ? Collections.emptyList() : list;
    }

    public void setList(List<Vod> list) {
        this.list = list;
    }

    public LinkedHashMap<String, List<Filter>> getFilters() {
        return filters == null ? new LinkedHashMap<>() : filters;
    }

    public List<List<Vod>> partition() {
        if (getList().size() % 6 == 0) return Lists.partition(getList(), 6);
        else return Lists.partition(getList(), 5);
    }

    @NonNull
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    static class FiltersAdapter implements JsonDeserializer<LinkedHashMap<String, List<Filter>>> {

        @Override
        public LinkedHashMap<String, List<Filter>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            LinkedHashMap<String, List<Filter>> filterMap = new LinkedHashMap<>();
            JsonObject filters = (JsonObject) json.getAsJsonObject();
            if (filters == null) return filterMap;
            for (String key : filters.keySet()) {
                List<Filter> items = new ArrayList<>();
                JsonElement element = filters.get(key);
                if (element.isJsonObject()) items.add(Filter.objectFrom(element));
                else for (JsonElement item : element.getAsJsonArray()) items.add(Filter.objectFrom(item));
                filterMap.put(key, items);
            }
            return filterMap;
        }
    }
}
