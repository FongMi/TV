package com.fongmi.quickjs.utils;

import android.net.Uri;

import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Asset;
import com.github.catvod.utils.Path;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

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

    public String fetch(String name) {
        if (cache.contains(name)) return cache.get(name);
        if (name.startsWith("http")) cache.put(name, request(name));
        if (name.startsWith("assets")) cache.put(name, Asset.read(name));
        if (name.startsWith("lib/")) cache.put(name, Asset.read("js/" + name));
        return cache.get(name);
    }

    private String request(String url) {
        try {
            Uri uri = Uri.parse(url);
            byte[] data = OkHttp.bytes(url);
            File file = Path.js(uri.getLastPathSegment());
            boolean cache = !"127.0.0.1".equals(uri.getHost());
            if (cache) new Thread(() -> Path.write(file, data)).start();
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return cache(url);
        }
    }

    private String cache(String url) {
        try {
            Uri uri = Uri.parse(url);
            File file = Path.js(uri.getLastPathSegment());
            return file.exists() ? Path.read(file) : "";
        } catch (Exception e) {
            return "";
        }
    }
}
