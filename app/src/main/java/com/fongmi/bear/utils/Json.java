package com.fongmi.bear.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Json {

    public static String safeString(JsonObject obj, String key, String value) {
        if (obj.has(key)) return obj.getAsJsonPrimitive(key).getAsString().trim();
        else return value;
    }

    public static int safeInt(JsonObject obj, String key, int value) {
        if (obj.has(key)) return obj.getAsJsonPrimitive(key).getAsInt();
        else return value;
    }

    public static List<String> safeList(JsonObject obj, String key) {
        List<String> result = new ArrayList<>();
        if (!obj.has(key)) return result;
        if (obj.get(key).isJsonObject()) result.add(obj.get(key).getAsString());
        else for (JsonElement opt : obj.getAsJsonArray(key)) result.add(opt.getAsString());
        return result;
    }
}
