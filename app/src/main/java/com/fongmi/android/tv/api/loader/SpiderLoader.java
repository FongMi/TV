package com.fongmi.android.tv.api.loader;

import android.content.Context;

import com.fongmi.android.tv.exception.SpiderLoadException;
import com.github.catvod.crawler.Spider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

abstract class SpiderLoader {

    private final Map<String, Spider> spiders = new HashMap<>();
    private final Map<String, FutureTask<Spider>> loading = new HashMap<>();
    private volatile boolean closed;
    private boolean destroyed;

    public final void clear() {
        throwFailure(closeLoader());
    }

    public final void destroy() {
        Throwable failure = closeLoader();
        List<Spider> active;
        synchronized (this) {
            destroyed = true;
            active = new ArrayList<>(spiders.values());
            spiders.clear();
        }
        for (Spider spider : active) failure = destroySpider(spider, failure);
        if (!active.isEmpty()) failure = clearReferences(failure);
        throwFailure(failure);
    }

    private Throwable closeLoader() {
        List<FutureTask<Spider>> pending;
        List<Spider> active;
        synchronized (this) {
            if (closed) return null;
            closed = true;
            pending = new ArrayList<>(loading.values());
            active = new ArrayList<>(spiders.values());
            loading.clear();
        }
        for (FutureTask<Spider> task : pending) task.cancel(true);
        Throwable failure = null;
        for (Spider spider : active) failure = closeSpider(spider, failure);
        return clearReferences(failure);
    }

    private Throwable clearReferences(Throwable failure) {
        try {
            clearReferences();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
        return failure;
    }

    protected void clearReferences() {
    }

    protected final Spider load(String key, Context context, String ext, Callable<Spider> factory) {
        FutureTask<Spider> task;
        synchronized (this) {
            checkOpen();
            Spider spider = spiders.get(key);
            if (spider != null) return spider;
            task = loading.computeIfAbsent(key, k -> new FutureTask<>(() -> createSpider(k, context, ext, factory)));
        }
        try {
            task.run();
            return awaitSpider(task);
        } finally {
            synchronized (this) {
                if (task.isDone()) loading.remove(key, task);
            }
        }
    }

    private Spider awaitSpider(FutureTask<Spider> task) {
        try {
            Spider spider = task.get();
            checkOpen();
            return spider;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new SpiderLoadException(error);
        } catch (ExecutionException error) {
            if (error.getCause() instanceof Error fatal) throw fatal;
            throw new SpiderLoadException(error.getCause());
        }
    }

    protected final synchronized void checkOpen() {
        if (closed) throw new CancellationException("Crawler loader is closed");
    }

    protected final synchronized Spider findSpider(String key) {
        checkOpen();
        return spiders.get(key);
    }

    private Spider createSpider(String key, Context context, String ext, Callable<Spider> factory) throws Exception {
        Spider spider = factory.call();
        if (!closed) spider.init(context, ext);
        cacheSpider(key, spider);
        return spider;
    }

    private void cacheSpider(String key, Spider spider) {
        synchronized (this) {
            if (!closed) {
                spiders.put(key, spider);
                return;
            }
        }
        retireSpider(key, spider);
    }

    private void retireSpider(String key, Spider spider) {
        Throwable failure = closeSpider(spider, null);
        boolean exiting;
        synchronized (this) {
            exiting = destroyed;
            if (!exiting) spiders.put(key, spider);
        }
        if (exiting) {
            failure = destroySpider(spider, failure);
            failure = clearReferences(failure);
        }
        CancellationException error = new CancellationException("Crawler loader is closed");
        if (failure != null) error.addSuppressed(failure);
        throw error;
    }

    private Throwable closeSpider(Spider spider, Throwable failure) {
        try {
            spider.close();
        } catch (RuntimeException | Error error) {
            if (failure == null) failure = error;
            else if (failure != error) failure.addSuppressed(error);
        }
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

    private void throwFailure(Throwable failure) {
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }
}
