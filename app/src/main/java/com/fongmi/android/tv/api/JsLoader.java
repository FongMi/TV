package com.fongmi.android.tv.api;

import com.fongmi.android.tv.App;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.DexClassLoader;

public class JsLoader {

    private final ConcurrentHashMap<String, Spider> spiders;
    private final JarLoader jarLoader;
    private String recent;

    public JsLoader() {
        jarLoader = new JarLoader();
        spiders = new ConcurrentHashMap<>();
    }

    public void clear() {
        for (Spider spider : spiders.values()) spider.destroy();
        jarLoader.clear();
        spiders.clear();
    }

    public void setRecent(String recent) {
        this.recent = recent;
    }

    private DexClassLoader dex(String key, String jar) {
        try {
            return jar.isEmpty() ? null : jarLoader.getLoader(key, jar);
        } catch (Throwable e) {
            return null;
        }
    }

    public Spider getSpider(String key, String api, String ext, String jar) {
        try {
            if (spiders.containsKey(key)) return spiders.get(key);
            Spider spider = new com.hiker.drpy.Spider(api, dex(key, jar));
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
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
}
