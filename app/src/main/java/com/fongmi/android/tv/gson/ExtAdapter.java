package com.fongmi.android.tv.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class ExtAdapter implements JsonDeserializer<String> {

    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) return json.getAsJsonPrimitive().getAsString();
        if (json.isJsonArray() || json.isJsonObject()) return json.toString();
        return "";
    }
}
