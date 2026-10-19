package com.fongmi.android.tv.api.loader;

import com.fongmi.android.tv.App;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;
import com.github.catvod.runtime.QuickJsRuntime;
import com.github.catvod.utils.RuntimeModule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import dalvik.system.DexClassLoader;

public class JsLoader {

    private final ConcurrentHashMap<String, Spider> spiders;
    private final ReentrantReadWriteLock lifecycle;
    private final QuickJsRuntime loader;
    private final QuickJsRuntime.Session module;
    private volatile String recent;

    public JsLoader() {
        this(RuntimeModule.QUICKJS.entry(QuickJsRuntime.class));
    }

    JsLoader(JsLoader previous) {
        this(previous.loader);
    }

    private JsLoader(QuickJsRuntime loader) {
        spiders = new ConcurrentHashMap<>();
        lifecycle = new ReentrantReadWriteLock();
        module = loader.session();
        this.loader = loader;
    }

    public void clear() {
        lifecycle.writeLock().lock();
        try {
            Throwable failure = destroySpiders();
            failure = clearModule(failure);
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

    private Throwable clearModule(Throwable failure) {
        try {
            module.clear();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        return failure;
    }

    public void setRecent(String recent) {
        this.recent = recent;
    }

    public Spider getSpider(String key, String api, String ext, String jar) {
        lifecycle.readLock().lock();
        try {
            return spiders.computeIfAbsent(key, k -> createSpider(k, api, ext, jar));
        } finally {
            lifecycle.readLock().unlock();
        }
    }

    private Spider createSpider(String key, String api, String ext, String jar) {
        Spider spider = null;
        try {
            spider = newSpider(key, api, BaseLoader.get().dex(jar));
            spider.init(App.get(), ext);
            return spider;
        } catch (Throwable e) {
            if (spider != null) spider.destroy();
            RuntimeModule.MissingException missing = RuntimeModule.MissingException.find(e);
            if (missing != null) throw missing;
            e.printStackTrace();
            return new SpiderNull();
        }
    }

    Spider newSpider(String key, String api, DexClassLoader dex) {
        Spider spider = module.spider(api, dex, JsHost.INSTANCE);
        spider.siteKey = key;
        return spider;
    }

    void cancel() {
        module.clear();
    }

    public Object[] proxy(Map<String, String> params) throws Exception {
        String key = recent;
        if (key == null) return null;
        Spider spider = spiders.get(key);
        return spider != null ? spider.proxy(params) : null;
    }
}
