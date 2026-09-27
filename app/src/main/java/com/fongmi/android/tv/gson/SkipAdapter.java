package com.fongmi.android.tv.gson;

import com.fongmi.android.tv.bean.SkipSegment;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkipAdapter implements JsonDeserializer<List<SkipSegment>> {

    @Override
    public List<SkipSegment> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json == null || !json.isJsonArray()) return Collections.emptyList();
        List<SkipSegment> segments = new ArrayList<>();
        for (JsonElement item : json.getAsJsonArray()) {
            if (!item.isJsonObject()) continue;
            try {
                SkipSegment segment = context.deserialize(item, SkipSegment.class);
                if (segment != null && segment.isValid()) segments.add(segment);
            } catch (JsonParseException ignored) {
                // Invalid optional skip data must not invalidate the playback URL.
            }
        }
        return segments;
    }
}
