package com.fongmi.quickjs.utils;

import android.net.Uri;

import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Headers;
import okhttp3.Response;

public class Module {

    private final ConcurrentHashMap<String, String> cache;

    private static class Loader {
        static volatile Module INSTANCE = new Module();
    }

    public static Module get() {
        return Loader.INSTANCE;
    }

    public Module() {
        this.cache = new ConcurrentHashMap<>();
    }

    public String load(String name) {
        if (cache.contains(name)) return cache.get(name);
        if (name.startsWith("http")) cache.put(name, getModule(name));
        if (name.startsWith("assets://")) cache.put(name, Path.asset(name.substring(9)));
        return cache.get(name);
    }

    private String getModule(String url) {
        try {
            Uri uri = Uri.parse(url);
            File file = Path.js(uri.getLastPathSegment());
            if (file.exists()) return Path.read(file);
            Response response = OkHttp.newCall(url, Headers.of("User-Agent", "Mozilla/5.0")).execute();
            if (response.code() != 200) return "";
            byte[] data = response.body().bytes();
            boolean cache = !"127.0.0.1".equals(uri.getHost());
            if (cache) new Thread(() -> Path.write(file, data)).start();
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
