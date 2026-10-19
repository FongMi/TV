package com.fongmi.android.tv.bean;

import com.fongmi.android.tv.App;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import okhttp3.HttpUrl;

public class Web {

    @SerializedName("url")
    private String url;
    @SerializedName("mode")
    private String mode;
    @SerializedName("cookie")
    private List<Cookie> cookie;

    public static Web objectFrom(String json) {
        try {
            Web web = App.gson().fromJson(json, Web.class);
            return web != null && web.isValid() ? web : null;
        } catch (JsonParseException e) {
            return null;
        }
    }

    public String getUrl() {
        return url;
    }

    public boolean isView() {
        return "view".equals(mode);
    }

    public boolean hasCookies() {
        return cookie != null && !cookie.isEmpty();
    }

    public boolean isValid() {
        if (url == null || HttpUrl.parse(url) == null) return false;
        if (mode != null && !"browser".equals(mode) && !"view".equals(mode)) return false;
        if (cookie == null) return true;
        return cookie.stream().allMatch(this::isValidCookie);
    }

    private boolean isValidCookie(Cookie group) {
        if (group == null) return false;
        if (group.url == null) group.url = url;
        if (HttpUrl.parse(group.url) == null || group.keys == null || group.keys.isEmpty()) return false;
        return group.keys.stream().allMatch(Web::validKey);
    }

    public void snapshot(Function<String, String> reader) {
        if (cookie == null) return;
        for (Cookie group : cookie) snapshot(group, reader);
    }

    private void snapshot(Cookie group, Function<String, String> reader) {
        Map<String, String> current = values(reader.apply(group.url));
        group.initial = new HashMap<>();
        for (String key : group.keys) group.initial.put(key, current.getOrDefault(key, ""));
    }

    public boolean isComplete(String page, Function<String, String> reader) {
        return cookie != null && cookie.stream().anyMatch(group -> isComplete(group, page, reader));
    }

    private boolean isComplete(Cookie group, String page, Function<String, String> reader) {
        if (group.initial == null || !sameOrigin(group.url, page)) return false;
        Map<String, String> current = values(reader.apply(group.url));
        boolean ready = true;
        boolean changed = false;
        for (String key : group.keys) {
            String value = current.getOrDefault(key, "");
            ready &= !value.isEmpty();
            changed |= !value.equals(group.initial.getOrDefault(key, ""));
        }
        return ready && changed;
    }

    private static boolean validKey(String key) {
        if (key == null || key.isEmpty()) return false;
        for (int i = 0; i < key.length(); i++) {
            char value = key.charAt(i);
            if (value <= 0x20 || value >= 0x7F || "()<>@,;:\\\"/[]?={}".indexOf(value) >= 0) return false;
        }
        return true;
    }

    private static boolean sameOrigin(String url, String page) {
        HttpUrl origin = HttpUrl.parse(url);
        HttpUrl current = page == null ? null : HttpUrl.parse(page);
        return origin != null && current != null && origin.scheme().equals(current.scheme()) && origin.host().equals(current.host()) && origin.port() == current.port();
    }

    private static Map<String, String> values(String header) {
        if (header == null) return Collections.emptyMap();
        Map<String, String> values = new HashMap<>();
        for (String item : header.split(";")) {
            int separator = item.indexOf('=');
            if (separator > 0) values.put(item.substring(0, separator).trim(), item.substring(separator + 1).trim());
        }
        return values;
    }

    private static class Cookie {
        @SerializedName("url")
        private String url;
        @SerializedName("keys")
        private List<String> keys;
        private transient Map<String, String> initial;
    }
}
