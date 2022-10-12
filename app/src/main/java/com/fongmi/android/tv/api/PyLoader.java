package com.fongmi.android.tv.api;

import android.content.Context;
import android.net.Uri;

import com.fongmi.android.tv.App;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class PyLoader {

    private final ConcurrentHashMap<String, Spider> spiders;
    private Object loader;

    public PyLoader() {
        spiders = new ConcurrentHashMap<>();
        init();
    }

    public void clear() {
        this.spiders.clear();
    }

    private void init() {
        try {
            loader = Class.forName("com.undcover.freedom.pyramid.Loader").newInstance();
            Method method = loader.getClass().getMethod("init", Context.class);
            method.invoke(loader, App.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Spider getSpider(String key, String api, String ext) {
        try {
            if (spiders.containsKey(key)) return spiders.get(key);
            String extend = Uri.parse(ext).getQueryParameter("extend");
            Method method = loader.getClass().getMethod("spider", String.class);
            Spider spider = (Spider) method.invoke(loader, ext);
            extend = extend == null ? "" : extend;
            spider.init(App.get(), extend);
            spiders.put(key, spider);
            return spider;
        } catch (Exception e) {
            e.printStackTrace();
            return new SpiderNull();
        }
    }
}
