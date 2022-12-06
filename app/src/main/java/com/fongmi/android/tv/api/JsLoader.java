package com.fongmi.android.tv.api;

import com.github.catvod.crawler.Spider;

import java.util.concurrent.ConcurrentHashMap;

public class JsLoader {

    private final ConcurrentHashMap<String, Spider> spiders;

    public JsLoader() {
        spiders = new ConcurrentHashMap<>();
        init();
    }

    public void clear() {
        this.spiders.clear();
    }

    private void init() {

    }
}
