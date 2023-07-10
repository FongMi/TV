package com.fongmi.android.tv.api;

import com.fongmi.android.tv.App;
import com.hiker.drpy.Loader;
import com.hiker.drpy.Spider;

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
            Spider spider = new Spider(api);
            spider.init(App.get(), ext);
            spiders.put(key, spider);
            return spider;
        } catch (Throwable e) {
            e.printStackTrace();
            return new Spider();
        }
    }

    public Object[] proxyInvoke(Map<?, ?> params) {
        try {
            Spider spider = spiders.get(recent);
            if (spider != null) return spider.doProxy(params);
            else return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
