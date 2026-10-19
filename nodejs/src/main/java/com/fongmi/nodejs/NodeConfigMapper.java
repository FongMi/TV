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
        JsonObject result = root.getAsJsonObject("video");
        JsonArray sites = mapSites(result);
        result.add("sites", sites);
        return result.toString();
    }

    private static JsonArray mapSites(JsonObject config) {
        JsonArray sites = new JsonArray();
        if (config.has("sites") && config.get("sites").isJsonArray()) for (JsonElement element : config.getAsJsonArray("sites")) addSite(sites, element);
        return sites;
    }

    private static void addSite(JsonArray sites, JsonElement element) {
        if (!element.isJsonObject()) return;
        JsonObject site = element.getAsJsonObject();
        if (!isEnabled(site.get("enable"))) return;
        String route = route(site);
        if (route.isEmpty()) return;
        site.addProperty("type", 3);
        site.addProperty("api", "node:" + route);
        sites.add(site);
    }

    private static boolean isEnabled(JsonElement enabled) {
        if (enabled == null || !enabled.isJsonPrimitive()) return true;
        if (enabled.getAsJsonPrimitive().isBoolean()) return enabled.getAsBoolean();
        if (enabled.getAsJsonPrimitive().isNumber()) return enabled.getAsInt() != 0;
        String value = enabled.getAsString();
        return !"false".equalsIgnoreCase(value) && !"0".equals(value);
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
}
