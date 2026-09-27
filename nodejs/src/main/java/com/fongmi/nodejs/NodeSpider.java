package com.fongmi.nodejs;

import android.content.Context;

import com.github.catvod.crawler.Spider;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NodeSpider extends Spider {

    private static final String LEGACY_CONFIG_ROUTE = "/spider/baseset/3";
    private static final Gson GSON = new Gson();
    private final String api;
    private final NodeClient client;
    private volatile Boolean videoCheck;

    public NodeSpider(NodeClient client, String api) {
        this.client = client;
        this.api = api;
    }

    @Override
    public void init(Context context, String extend) throws Exception {
        JsonObject body = new JsonObject();
        String value = extend == null ? "" : extend;
        body.addProperty("ext", value);
        body.addProperty("extend", value);
        client.initialize(api, body);
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("filter", filter);
        return addLegacyConfigActions(call("/home", body));
    }

    @Override
    public String homeVideoContent() throws Exception {
        String result = client.postOptional(NodeRoute.append(api, "/homeVod"), new JsonObject());
        if (result.isEmpty()) result = client.postOptional(NodeRoute.append(api, "/homeVideo"), new JsonObject());
        return addLegacyConfigActions(result);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("id", tid);
        body.addProperty("page", pg);
        body.addProperty("filter", filter);
        body.add("filters", GSON.toJsonTree(extend));
        return addLegacyConfigActions(call("/category", body));
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        return detail(ids.isEmpty() ? "" : ids.get(0));
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        return searchContent(key, quick, "1");
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("wd", key);
        body.addProperty("quick", quick);
        body.addProperty("page", pg);
        return call("/search", body);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        client.setPlayInfo(flag, id);
        JsonObject body = new JsonObject();
        body.addProperty("flag", flag);
        body.addProperty("id", id);
        body.add("flags", GSON.toJsonTree(vipFlags));
        return call("/play", body);
    }

    @Override
    public String liveContent(String url) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("url", url);
        return client.postOptional(NodeRoute.append(api, "/live"), body);
    }

    @Override
    public boolean manualVideoCheck() throws Exception {
        if (videoCheck != null) return videoCheck;
        JsonObject body = new JsonObject();
        body.addProperty("clip", "");
        String result = client.postOptional(NodeRoute.append(api, "/support"), body);
        return videoCheck = !result.isEmpty();
    }

    @Override
    public boolean isVideoFormat(String url) throws Exception {
        if (!manualVideoCheck()) return false;
        JsonObject body = new JsonObject();
        body.addProperty("clip", url);
        String result = client.postOptional(NodeRoute.append(api, "/support"), body);
        if (result.isEmpty()) {
            videoCheck = false;
            return false;
        }
        return JsonParser.parseString(result).getAsBoolean();
    }

    @Override
    public Object[] proxy(Map<String, String> params) throws Exception {
        return client.proxy(api, params);
    }

    @Override
    public String action(String action) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("action", action);
        String result = client.postOrNull(NodeRoute.append(api, "/action"), body);
        if (result != null) return result;
        return isLegacyConfigSpider() ? detail(action) : "";
    }

    private String addLegacyConfigActions(String result) {
        return isLegacyConfigSpider() ? addMissingActions(result) : result;
    }

    private boolean isLegacyConfigSpider() {
        String route = NodeRoute.path(api);
        return route.equals(LEGACY_CONFIG_ROUTE) || route.startsWith(LEGACY_CONFIG_ROUTE + "?");
    }

    static String addMissingActions(String result) {
        try {
            JsonObject root = JsonParser.parseString(result).getAsJsonObject();
            JsonArray list = root.getAsJsonArray("list");
            if (list == null) return result;
            boolean changed = false;
            for (JsonElement element : list) {
                if (!element.isJsonObject()) continue;
                JsonObject item = element.getAsJsonObject();
                JsonElement id = item.get("vod_id");
                if (id == null || !id.isJsonPrimitive() || id.getAsString().isEmpty() || item.has("action")) continue;
                item.addProperty("action", id.getAsString());
                changed = true;
            }
            return changed ? GSON.toJson(root) : result;
        } catch (RuntimeException e) {
            return result;
        }
    }

    private String detail(String id) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("id", id);
        return call("/detail", body);
    }

    private String call(String route, JsonObject body) throws Exception {
        return client.post(NodeRoute.append(api, route), body);
    }
}
