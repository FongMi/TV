package com.fongmi.android.tv.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.List;

public class UrlAdapter implements JsonDeserializer<String> {

    @Override
    public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonArray()) return json.getAsString();
        List<JsonElement> elements = json.getAsJsonArray().asList();
        return elements.size() < 2 ? "" : elements.get(1).getAsString();
    }
}
