package com.github.catvod.net;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class NetOptions {

    private static final Gson GSON = new Gson();

    @SerializedName("buffer")
    private Integer buffer;
    @SerializedName("redirect")
    private Integer redirect;
    @SerializedName("timeout")
    private Integer timeout;
    @SerializedName("callTimeout")
    private Integer callTimeout;
    @SerializedName("postType")
    private String postType;
    @SerializedName("method")
    private String method;
    @SerializedName("body")
    private String body;
    @SerializedName("data")
    private JsonElement data;
    @SerializedName("headers")
    private JsonElement headers;
    @SerializedName("params")
    private JsonElement params;
    @SerializedName("cookie")
    private Boolean cookie;
    @SerializedName("ping")
    private Integer ping;
    @SerializedName("idle")
    private Integer idle;
    @SerializedName("reconnect")
    private Boolean reconnect;
    @SerializedName("maxBytes")
    private Long maxBytes;
    @SerializedName("size")
    private Long size;
    @SerializedName("threads")
    private Integer threads;

    public long getMaxBytes() {
        if (maxBytes != null && maxBytes <= 0) throw new IllegalArgumentException("Invalid download size limit");
        return maxBytes == null ? Long.MAX_VALUE : maxBytes;
    }

    public long getSize() {
        if (size != null && size <= 0) throw new IllegalArgumentException("Invalid download size");
        return size == null ? -1 : size;
    }

    public int getThreads() {
        if (threads != null && (threads < 0 || threads > 4)) throw new IllegalArgumentException("Invalid download threads");
        return threads == null ? 0 : threads;
    }

    public static NetOptions from(String json) {
        NetOptions options = json == null || json.isEmpty() ? null : GSON.fromJson(json, NetOptions.class);
        return options == null ? new NetOptions() : options;
    }

    static NetOptions text(String method, String body, Map<String, String> headers, int timeout) {
        NetOptions options = new NetOptions();
        JsonObject values = headers == null ? new JsonObject() : GSON.toJsonTree(headers).getAsJsonObject();
        if (method.equals("POST") && values.keySet().stream().noneMatch(key -> key.equalsIgnoreCase("Content-Type"))) values.addProperty("Content-Type", "application/json; charset=utf-8");
        options.method = method;
        options.body = body;
        options.headers = values;
        options.timeout = timeout;
        return options;
    }

    public int getBuffer() {
        return buffer == null ? 0 : buffer;
    }

    public int getRedirect() {
        return redirect == null ? 1 : redirect;
    }

    public int getTimeout() {
        return timeout == null ? 10000 : timeout;
    }

    public int getCallTimeout() {
        if (callTimeout != null && callTimeout < 0) throw new IllegalArgumentException("callTimeout must not be negative");
        return callTimeout == null ? 0 : callTimeout;
    }

    public int getPing() {
        if (ping != null && ping < 0) throw new IllegalArgumentException("ping must not be negative");
        return ping == null ? 0 : ping;
    }

    public int getIdle() {
        if (idle != null && idle < 0) throw new IllegalArgumentException("idle must not be negative");
        return idle == null ? 0 : idle;
    }

    public boolean isReconnect() {
        return Boolean.TRUE.equals(reconnect);
    }

    public String getPostType() {
        return postType == null || postType.isEmpty() ? "json" : postType;
    }

    public String getMethod() {
        return method == null || method.isEmpty() ? "get" : method;
    }

    public String getBody() {
        return body;
    }

    public JsonElement getData() {
        return data;
    }

    String message() {
        if (data == null) return body;
        if (data.isJsonPrimitive() && data.getAsJsonPrimitive().isString()) return data.getAsString();
        return data.toString();
    }

    public JsonElement getParams() {
        return params;
    }

    public boolean isRedirect() {
        return getRedirect() == 1;
    }

    public boolean isCookie() {
        return Boolean.TRUE.equals(cookie);
    }

    public Map<String, String> getHeader() {
        return toMap(headers);
    }

    static Map<String, String> toMap(JsonElement element) {
        Map<String, String> values = new HashMap<>();
        JsonObject object;
        try {
            if (element != null && element.isJsonPrimitive()) element = JsonParser.parseString(element.getAsString());
            object = element.getAsJsonObject();
        } catch (RuntimeException e) {
            return values;
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) values.put(entry.getKey(), textValue(entry.getValue()));
        return values;
    }

    private static String textValue(JsonElement value) {
        try {
            return value.getAsJsonPrimitive().getAsString().trim();
        } catch (RuntimeException e) {
            return "";
        }
    }

    public String getCharset() {
        for (Map.Entry<String, String> entry : getHeader().entrySet()) {
            if (!entry.getKey().equalsIgnoreCase("Content-Type")) continue;
            String charset = findCharset(entry.getValue());
            if (charset != null) return charset;
        }
        return "UTF-8";
    }

    private String findCharset(String contentType) {
        for (String part : contentType.split(";")) {
            String text = part.trim();
            if (text.startsWith("charset=")) return text.substring(8).replace("\"", "");
        }
        return null;
    }
}
