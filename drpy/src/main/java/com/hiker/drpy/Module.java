package com.hiker.drpy;

import com.hiker.drpy.net.OkHttp;

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

    public String load(String name) {
        if (cache.contains(name)) return cache.get(name);
        String content = OkHttp.get().module(name);
        cache.put(name, content);
        return content;
    }
}
