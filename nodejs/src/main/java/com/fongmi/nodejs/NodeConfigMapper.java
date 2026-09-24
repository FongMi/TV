package com.fongmi.nodejs;

import com.github.catvod.utils.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class NodeConfigMapper {

    private NodeConfigMapper() {
    }

    public static String transform(String json) throws Exception {
        JsonObject root = Json.parse(json).getAsJsonObject();
        if (root.has("data") && root.get("data").isJsonObject()) root = root.getAsJsonObject("data");
        if (!root.has("video") || !root.get("video").isJsonObject()) throw new Exception("Node /config has no video object");
        JsonObject result = root.getAsJsonObject("video").deepCopy();
        JsonArray sites = new JsonArray();
        if (result.has("sites") && result.get("sites").isJsonArray()) {
            for (JsonElement element : result.getAsJsonArray("sites")) {
                if (!element.isJsonObject()) continue;
                JsonObject site = element.getAsJsonObject().deepCopy();
                if (!isEnabled(site, "enable")) continue;
                String route = route(site);
                if (route.isEmpty()) continue;
                site.addProperty("type", 3);
                site.addProperty("api", "node:" + route);
                sites.add(site);
            }
        }
        result.add("sites", sites);
        return result.toString();
    }

    static Probe firstProbe(String json) {
        try {
            JsonObject root = Json.parse(json).getAsJsonObject();
            if (!root.has("sites") || !root.get("sites").isJsonArray()) return Probe.EMPTY;
            Probe fallback = Probe.EMPTY;
            for (JsonElement element : root.getAsJsonArray("sites")) {
                if (!element.isJsonObject()) continue;
                JsonObject site = element.getAsJsonObject();
                String api = Json.safeString(site, "api");
                if (!api.startsWith("node:")) continue;
                Probe probe = new Probe(api, extension(site));
                if (fallback.api().isEmpty()) fallback = probe;
                if (isEnabled(site, "indexs")) return probe;
            }
            return fallback;
        } catch (Exception e) {
            return Probe.EMPTY;
        }
    }

    public static String firstProbeApi(String json) {
        return firstProbe(json).api();
    }

    private static boolean isEnabled(JsonObject site, String name) {
        JsonElement value = site.get(name);
        if (value == null || !value.isJsonPrimitive()) return true;
        if (value.getAsJsonPrimitive().isBoolean()) return value.getAsBoolean();
        if (value.getAsJsonPrimitive().isNumber()) return value.getAsInt() != 0;
        String text = value.getAsString();
        return !"false".equalsIgnoreCase(text) && !"0".equals(text);
    }

    private static String extension(JsonObject site) {
        JsonElement ext = site.get("ext");
        if (ext == null || ext.isJsonNull()) return "";
        return ext.isJsonPrimitive() ? ext.getAsString() : ext.toString();
    }

    private static String route(JsonObject site) {
        String api = Json.safeString(site, "api");
        if (!api.isEmpty()) {
            int scheme = api.indexOf("://");
            if (scheme >= 0) {
                int slash = api.indexOf('/', scheme + 3);
                api = slash < 0 ? "/" : api.substring(slash);
            }
            return api.startsWith("/") ? api : "/" + api;
        }
        String key = Json.safeString(site, "key");
        if (key.startsWith("nodejs_")) key = key.substring(7);
        if (key.isEmpty()) return "";
        int type = site.has("type") ? site.get("type").getAsInt() : 3;
        return "/spider/" + key + "/" + type;
    }

    record Probe(String api, String ext) {
        private static final Probe EMPTY = new Probe("", "");
    }
}
