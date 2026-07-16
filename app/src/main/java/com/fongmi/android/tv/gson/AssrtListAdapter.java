package com.fongmi.android.tv.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AssrtListAdapter implements JsonDeserializer<List<?>> {

    @Override
    public List<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json == null || json.isJsonNull()) return Collections.emptyList();
        if (!(typeOfT instanceof ParameterizedType type)) throw new JsonParseException("Expected a parameterized list type");
        Type itemType = type.getActualTypeArguments()[0];
        List<Object> items = new ArrayList<>();
        if (json.isJsonArray()) for (JsonElement element : json.getAsJsonArray()) add(items, element, itemType, context);
        else if (json.isJsonObject() && !json.getAsJsonObject().entrySet().isEmpty()) add(items, json, itemType, context);
        return items;
    }

    private void add(List<Object> items, JsonElement element, Type itemType, JsonDeserializationContext context) {
        Object item = context.deserialize(element, itemType);
        if (item != null) items.add(item);
    }
}
