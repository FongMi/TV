package com.fongmi.bear.utils;

import com.google.gson.JsonObject;

public class Json {

    public static String safeString(JsonObject obj, String key, String value) {
        if (obj.has(key)) return obj.getAsJsonPrimitive(key).getAsString().trim();
        else return value;
    }

    public static int safeInt(JsonObject obj, String key, int value) {
        if (obj.has(key)) return obj.getAsJsonPrimitive(key).getAsInt();
        else return value;
    }
}
