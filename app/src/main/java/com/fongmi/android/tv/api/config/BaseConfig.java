package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.bean.Header;
import com.github.catvod.bean.Proxy;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Task;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

abstract class BaseConfig {

    public static final int VOD = 0;
    public static final int LIVE = 1;
    public static final int WALL = 2;

    private final AtomicInteger taskId = new AtomicInteger(0);
    private final Object loadLock = new Object();

    protected boolean sync;
    protected volatile Config config;
    private volatile Future<?> future;

    protected abstract String getTag();

    protected abstract Config defaultConfig();

    protected abstract void load(Config config) throws Throwable;

    protected abstract boolean isLoaded();

    protected void cancelLoad() {
    }

    public synchronized void ensureLoaded() {
        try {
            if (isLoaded()) return;
            if (config == null) config = defaultConfig();
            Server.get().start();
            load(config);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected void postEvent() {
        ConfigEvent.common();
    }

    public boolean needSync(String url) {
        Config selected = getConfig();
        return sync || selected.isEmpty() || url.equals(selected.getUrl());
    }

    public Config getConfig() {
        return config == null ? defaultConfig() : config;
    }

    protected void setHeaders(List<Header> headers) {
        OkHttp.responseInterceptor().addAll(headers);
    }

    protected void setProxy(List<Proxy> proxy) {
        OkHttp.selector().addAll(proxy);
    }

    protected void setHosts(List<String> hosts) {
        OkHttp.dns().addAll(hosts);
    }

    public void load(Callback callback) {
        synchronized (loadLock) {
            int id = taskId.incrementAndGet();
            if (future != null && !future.isDone()) {
                cancelLoad();
                future.cancel(true);
            }
            future = Task.submit(() -> loadConfig(id, config, callback));
        }
        callback.start();
    }

    public void cancel() {
        synchronized (loadLock) {
            taskId.incrementAndGet();
            cancelLoad();
            OkHttp.cancel(getTag());
            if (future != null && !future.isDone()) future.cancel(true);
        }
    }

    protected void loadConfig(int id, Config config, Callback callback) {
        try {
            Server.get().start();
            OkHttp.cancel(getTag());
            load(config);
            if (taskId.get() != id) return;
            completeLoad(id, config, callback);
        } catch (Throwable e) {
            if (isCanceled(e)) return;
            e.printStackTrace();
            if (taskId.get() != id) return;
            App.post(() -> {
                if (taskId.get() == id) callback.error(TextUtils.isEmpty(config.getUrl()) ? "" : Notify.getError(R.string.error_config_get, e));
            });
        }
        if (taskId.get() == id) postEvent();
    }

    private void completeLoad(int id, Config config, Callback callback) {
        if (config.equals(this.config)) config.update();
        String notice = config.getNotice();
        App.post(() -> {
            if (taskId.get() != id) return;
            Notify.show(notice);
            callback.success();
        });
    }

    protected boolean isCanceled(Throwable e) {
        if (Thread.currentThread().isInterrupted()) return true;
        boolean canceled = false;
        boolean timedOut = false;
        for (Throwable current = e; current != null; current = current.getCause()) {
            if (current instanceof InterruptedException || current instanceof CancellationException) return true;
            canceled |= current instanceof InterruptedIOException || "Canceled".equals(current.getMessage());
            timedOut |= current instanceof SocketTimeoutException;
        }
        return canceled && !timedOut;
    }

    protected JsonArray fetchArray(JsonObject object, String key) {
        if (!object.has(key)) return new JsonArray();
        JsonElement element = object.get(key);
        if (element.isJsonObject()) return new JsonArray();
        if (element.isJsonPrimitive()) element = fetch(element.getAsString());
        JsonArray result = new JsonArray();
        for (JsonElement item : element.getAsJsonArray()) {
            if (item.isJsonPrimitive()) result.addAll(fetch(item.getAsString()));
            else if (item.isJsonObject()) result.add(item);
        }
        return result;
    }

    private JsonArray fetch(String url) {
        try {
            JsonElement parsed = Json.parse(OkHttp.string(UrlUtil.convert(url)));
            return parsed.isJsonArray() ? parsed.getAsJsonArray() : new JsonArray();
        } catch (Exception e) {
            return new JsonArray();
        }
    }
}
