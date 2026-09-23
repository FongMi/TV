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
                if (!isEnabled(site)) continue;
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
                if (isIndexed(site)) return probe;
            }
            return fallback;
        } catch (Exception e) {
            return Probe.EMPTY;
        }
    }

    public static String firstProbeApi(String json) {
        return firstProbe(json).api();
    }

    private static boolean isIndexed(JsonObject site) {
        if (!site.has("indexs") || !site.get("indexs").isJsonPrimitive()) return true;
        if (site.getAsJsonPrimitive("indexs").isBoolean()) return site.get("indexs").getAsBoolean();
        if (site.getAsJsonPrimitive("indexs").isNumber()) return site.get("indexs").getAsInt() != 0;
        String value = site.get("indexs").getAsString();
        return !"false".equalsIgnoreCase(value) && !"0".equals(value);
    }

    private static boolean isEnabled(JsonObject site) {
        if (!site.has("enable") || !site.get("enable").isJsonPrimitive()) return true;
        if (site.getAsJsonPrimitive("enable").isBoolean()) return site.get("enable").getAsBoolean();
        if (site.getAsJsonPrimitive("enable").isNumber()) return site.get("enable").getAsInt() != 0;
        return !"false".equalsIgnoreCase(site.get("enable").getAsString()) && !"0".equals(site.get("enable").getAsString());
    }

    private static String extension(JsonObject site) {
        if (!site.has("ext") || site.get("ext").isJsonNull()) return "";
        JsonElement ext = site.get("ext");
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
