package com.fongmi.android.tv.api.loader;

import com.fongmi.android.tv.App;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;
import com.github.catvod.runtime.PythonRuntime;
import com.github.catvod.utils.RuntimeModule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PyLoader {

    private final ConcurrentHashMap<String, Spider> spiders;
    private final ReentrantReadWriteLock lifecycle;
    private final PythonRuntime loader;
    private volatile String recent;

    public PyLoader() {
        this(RuntimeModule.PYTHON.entry(PythonRuntime.class));
    }

    PyLoader(PyLoader previous) {
        this(previous.loader);
    }

    private PyLoader(PythonRuntime loader) {
        loader.initialize();
        spiders = new ConcurrentHashMap<>();
        lifecycle = new ReentrantReadWriteLock();
        this.loader = loader;
    }

    public void clear() {
        lifecycle.writeLock().lock();
        try {
            Throwable failure = destroySpiders();
            spiders.clear();
            recent = null;
            if (failure instanceof RuntimeException error) throw error;
            if (failure instanceof Error error) throw error;
        } finally {
            lifecycle.writeLock().unlock();
        }
    }

    private Throwable destroySpiders() {
        Throwable failure = null;
        for (Spider spider : spiders.values()) failure = destroySpider(spider, failure);
        return failure;
    }

    private Throwable destroySpider(Spider spider, Throwable failure) {
        try {
            spider.destroy();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        return failure;
    }

    public void setRecent(String recent) {
        this.recent = recent;
    }

    public Spider getSpider(String key, String api, String ext) {
        lifecycle.readLock().lock();
        try {
            return spiders.computeIfAbsent(key, k -> createSpider(k, api, ext));
        } finally {
            lifecycle.readLock().unlock();
        }
    }

    private Spider createSpider(String key, String api, String ext) {
        Spider spider = null;
        try {
            spider = newSpider(key, api);
            spider.init(App.get(), ext);
            return spider;
        } catch (Throwable e) {
            if (spider != null) spider.destroy();
            e.printStackTrace();
            return new SpiderNull();
        }
    }

    Spider newSpider(String key, String api) {
        return loader.spider(api, key, JsHost.INSTANCE, JsHost.INSTANCE::getUserAgent);
    }

    public Object[] proxy(Map<String, String> params) throws Exception {
        String key = recent;
        if (key == null) return null;
        Spider spider = spiders.get(key);
        return spider != null ? spider.proxy(params) : null;
    }
}
