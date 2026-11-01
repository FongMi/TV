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
import java.util.concurrent.CancellationException;

public final class NodeSpider extends Spider {

    private static final String LEGACY_CONFIG_ROUTE = "/spider/baseset/3";
    private static final Gson GSON = new Gson();
    private final String api;
    private final NodeClient client;
    private final long session;
    private volatile Boolean videoCheck;
    private volatile boolean closed;

    public NodeSpider(NodeClient client, String api) {
        this.client = client;
        this.api = api;
        session = client.session();
    }

    static String addMissingActions(String result) {
        try {
            JsonObject root = JsonParser.parseString(result).getAsJsonObject();
            JsonArray list = root.getAsJsonArray("list");
            if (list == null) return result;
            boolean changed = addListActions(list);
            return changed ? GSON.toJson(root) : result;
        } catch (RuntimeException e) {
            return result;
        }
    }

    private static boolean addListActions(JsonArray list) {
        boolean changed = false;
        for (JsonElement element : list) changed |= addListAction(element);
        return changed;
    }

    private static boolean addListAction(JsonElement element) {
        if (!element.isJsonObject()) return false;
        JsonObject item = element.getAsJsonObject();
        JsonElement id = item.get("vod_id");
        if (id == null || !id.isJsonPrimitive() || id.getAsString().isEmpty() || item.has("action")) return false;
        item.addProperty("action", id.getAsString());
        return true;
    }

    @Override
    public void init(Context context, String extend) throws Exception {
        checkActive();
        JsonObject body = new JsonObject();
        String value = extend == null ? "" : extend;
        body.addProperty("ext", value);
        body.addProperty("extend", value);
        client.initialize(session, api, body);
    }

    @Override
    public void destroy() {
        close();
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        super.close();
    }

    private void checkActive() {
        if (closed) throw new CancellationException("Spider closed");
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("filter", filter);
        return addLegacyConfigActions(call("/home", body));
    }

    @Override
    public String homeVideoContent() throws Exception {
        checkActive();
        String result = client.postOptional(session, NodeRoute.append(api, "/homeVod"), new JsonObject());
        if (result.isEmpty()) result = client.postOptional(session, NodeRoute.append(api, "/homeVideo"), new JsonObject());
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
        checkActive();
        if (ids.isEmpty()) return super.detailContent(ids);
        String first = detail(ids.get(0));
        if (ids.size() == 1) return first;
        JsonObject result = JsonParser.parseString(first).getAsJsonObject();
        JsonArray list = result.getAsJsonArray("list");
        for (int i = 1; i < ids.size(); i++) appendDetail(list, ids.get(i));
        return result.toString();
    }

    private void appendDetail(JsonArray list, String id) throws Exception {
        JsonObject item = JsonParser.parseString(detail(id)).getAsJsonObject();
        list.addAll(item.getAsJsonArray("list"));
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
        checkActive();
        client.setPlayInfo(session, flag, id);
        JsonObject body = new JsonObject();
        body.addProperty("flag", flag);
        body.addProperty("id", id);
        body.add("flags", GSON.toJsonTree(vipFlags));
        return call("/play", body);
    }

    @Override
    public String liveContent(String url) throws Exception {
        checkActive();
        JsonObject body = new JsonObject();
        body.addProperty("url", url);
        return client.postOptional(session, NodeRoute.append(api, "/live"), body);
    }

    @Override
    public boolean manualVideoCheck() throws Exception {
        checkActive();
        if (videoCheck != null) return videoCheck;
        JsonObject body = new JsonObject();
        body.addProperty("clip", "");
        String result = client.postOptional(session, NodeRoute.append(api, "/support"), body);
        return videoCheck = !result.isEmpty();
    }

    @Override
    public boolean isVideoFormat(String url) throws Exception {
        if (!manualVideoCheck()) return false;
        JsonObject body = new JsonObject();
        body.addProperty("clip", url);
        String result = client.postOptional(session, NodeRoute.append(api, "/support"), body);
        if (result.isEmpty()) {
            videoCheck = false;
            return false;
        }
        return JsonParser.parseString(result).getAsBoolean();
    }

    @Override
    public Object[] proxy(Map<String, String> params) throws Exception {
        checkActive();
        return client.proxy(session, api, params);
    }

    @Override
    public String action(String action) throws Exception {
        checkActive();
        JsonObject body = new JsonObject();
        body.addProperty("action", action);
        String result = client.postOrNull(session, NodeRoute.append(api, "/action"), body);
        if (result == null) result = isLegacyConfigSpider() ? detail(action) : "";
        if (result.isEmpty()) return result;
        JsonObject response = JsonParser.parseString(result).getAsJsonObject();
        JsonElement web = response.get("web");
        if (web == null || !web.isJsonObject()) return result;
        response.add("web", client.normalizeWeb(web.getAsJsonObject()));
        return response.toString();
    }

    private String addLegacyConfigActions(String result) {
        return isLegacyConfigSpider() ? addMissingActions(result) : result;
    }

    private boolean isLegacyConfigSpider() {
        String route = NodeRoute.path(api);
        return route.equals(LEGACY_CONFIG_ROUTE) || route.startsWith(LEGACY_CONFIG_ROUTE + "?");
    }

    private String detail(String id) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("id", id);
        return call("/detail", body);
    }

    private String call(String route, JsonObject body) throws Exception {
        checkActive();
        return client.post(session, NodeRoute.append(api, route), body);
    }
}
