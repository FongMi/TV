package com.fongmi.android.tv.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class StringAdapter implements JsonDeserializer<String> {

    public static Gson gson() {
        return new GsonBuilder().registerTypeAdapter(String.class, new StringAdapter()).create();
    }

    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) return json.getAsJsonPrimitive().getAsString();
        if (json.isJsonArray() || json.isJsonObject()) return json.toString();
        return "";
    }
}
