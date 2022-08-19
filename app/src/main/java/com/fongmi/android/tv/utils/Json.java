package com.fongmi.android.tv.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Json {

    public static String safeString(JsonObject obj, String key, String value) {
        if (obj.has(key)) return obj.getAsJsonPrimitive(key).getAsString().trim();
        else return value;
    }

    public static List<String> safeList(JsonObject obj, String key) {
        List<String> result = new ArrayList<>();
        if (!obj.has(key)) return result;
        if (obj.get(key).isJsonObject()) result.add(obj.get(key).getAsString());
        else for (JsonElement opt : obj.getAsJsonArray(key)) result.add(opt.getAsString());
        return result;
    }

    public static JsonObject safeObject(JsonElement element) {
        try {
            if (element.isJsonPrimitive()) element = JsonParser.parseString(element.getAsJsonPrimitive().getAsString());
            return element.getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    public static HashMap<String, String> toMap(JsonElement element) {
        HashMap<String, String> map = new HashMap<>();
        JsonObject object = safeObject(element);
        if (object == null) return map;
        for (String key : object.keySet()) map.put(key, object.get(key).getAsString());
        return map;
    }
}
