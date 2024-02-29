package com.fongmi.android.tv.gson;

import com.fongmi.android.tv.bean.Url;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class UrlAdapter implements JsonDeserializer<Url> {

    @Override
    public Url deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonArray()) return convert(json.getAsJsonArray());
        if (json.isJsonObject()) return Url.objectFrom(json);
        return Url.create().add(json.getAsString());
    }

    private Url convert(JsonArray array) {
        Url url = Url.create();
        for (int i = 0; i < array.size(); i += 2) url.add(array.get(i).getAsString(), array.get(i + 1).getAsString());
        return url;
    }
}
