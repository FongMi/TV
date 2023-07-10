package com.fongmi.android.tv.api;

import com.fongmi.android.tv.App;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;
import com.hiker.drpy.Loader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JsLoader {

    private final ConcurrentHashMap<String, Spider> spiders;
    private String recent;

    public JsLoader() {
        spiders = new ConcurrentHashMap<>();
        init();
    }

    public void clear() {
        for (Spider spider : spiders.values()) spider.destroy();
        this.spiders.clear();
    }

    public void setRecent(String recent) {
        this.recent = recent;
    }

    private void init() {
        try {
            Loader.init(App.get());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public Spider getSpider(String key, String api, String ext) {
        try {
            if (spiders.containsKey(key)) return spiders.get(key);
            Spider spider = new com.hiker.drpy.Spider(api);
            spider.init(App.get(), ext);
            spiders.put(key, spider);
            return spider;
        } catch (Throwable e) {
            e.printStackTrace();
            return new SpiderNull();
        }
    }

    public Object[] proxyInvoke(Map<?, ?> params) {
        try {
            Spider spider = spiders.get(recent);
            if (spider != null) return spider.proxyLocal(params);
            else return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
