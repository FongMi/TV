package com.fongmi.android.tv.gson;

import com.fongmi.android.tv.bean.Filter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class FilterAdapter implements JsonDeserializer<LinkedHashMap<String, List<Filter>>> {

    public static Gson gson() {
        Type type = new TypeToken<LinkedHashMap<String, List<Filter>>>() {}.getType();
        return new GsonBuilder().registerTypeAdapter(type, new FilterAdapter()).create();
    }

    @Override
    public LinkedHashMap<String, List<Filter>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        LinkedHashMap<String, List<Filter>> filterMap = new LinkedHashMap<>();
        JsonObject filters = json.getAsJsonObject();
        if (filters == null) return filterMap;
        for (String key : filters.keySet()) {
            List<Filter> items = new ArrayList<>();
            JsonElement element = filters.get(key);
            if (element.isJsonObject()) items.add(Filter.objectFrom(element).trans());
            else for (JsonElement item : element.getAsJsonArray()) items.add(Filter.objectFrom(item).trans());
            filterMap.put(key, items);
        }
        return filterMap;
    }
}
