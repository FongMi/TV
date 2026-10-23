package com.fongmi.quickjs.method;

import android.net.Uri;

import com.fongmi.quickjs.QuickJS;
import com.fongmi.quickjs.WebPlatform;
import com.fongmi.quickjs.host.Host;
import com.fongmi.quickjs.utils.Connect;
import com.fongmi.quickjs.utils.HtmlQuery;
import com.github.catvod.Proxy;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.Net;
import com.github.catvod.net.NetOptions;
import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Js;
import com.github.catvod.utils.Local;
import com.github.catvod.utils.Trans;
import com.github.catvod.utils.UriUtil;
import com.google.gson.JsonElement;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Global {

    private final WebPlatform web;
    private final Network network;
    private final Cache cache;
    private final Streams streams;
    private final Sockets sockets;
    private final QuickJS ctx;
    private final Js js = new Js();
    private final Host host;
    private final String siteKey;
    private final Map<QuickJS.Callback, Invocation> callbacks = new HashMap<>();
    private final Map<Integer, Consumer<JSONObject>> danmakus = new ConcurrentHashMap<>();
    private final AtomicInteger nextDanmaku = new AtomicInteger();
    private Invocation invocation;

    private volatile boolean destroyed;

    private Global(QuickJS ctx, Host host, String siteKey, String api) {
        this.host = host;
        Net net = new Net(host, siteKey);
        Local local = new Local(siteKey);
        this.siteKey = siteKey;
        this.ctx = ctx;
        ctx.setAsyncScopeCapture(this::captureInvocation).join();
        this.web = new WebPlatform(ctx, SpiderDebug::log, api);
        this.network = new Network(this, net, web);
        this.cache = new Cache(this, network, local);
        this.streams = new Streams(this, network, host.getCallTimeout());
        this.sockets = new Sockets(this, network);
        setProperty();
        ctx.onClose(this::destroy).join();
    }

    public static Global create(QuickJS ctx, Host host, String siteKey, String api) {
        return new Global(ctx, host, siteKey, api);
    }

    private static Object argument(Object[] args, int index) {
        return index < args.length ? args[index] : null;
    }

    private static String string(Object[] args, int index) {
        return (String) argument(args, index);
    }

    private static Integer integer(Object[] args, int index) {
        Number value = (Number) argument(args, index);
        return value == null ? null : value.intValue();
    }

    static String errorMessage(Throwable error) {
        if (error == null) return null;
        if (error instanceof CompletionException && error.getCause() != null) error = error.getCause();
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        network.cancelSleeps();
        try {
            streams.close();
        } finally {
            try {
                network.close();
            } finally {
                release();
            }
        }
    }

    private void release() {
        js.close();
        danmakus.clear();
        network.release();
        cache.release();
        releaseCallbacks();
        sockets.release();
        web.close();
    }

    private void releaseCallbacks() {
        for (QuickJS.Callback callback : callbacks.keySet()) callback.close();
        callbacks.clear();
    }

    public void cancelRequests() {
        sockets.markClosing();
        try {
            streams.close();
        } finally {
            network.cancelSleeps();
            network.close();
        }
    }

    private Boolean closeSession(Integer id) {
        Net session = network.removeSession(id);
        sockets.markClosing(session);
        network.cancelSleeps(session);
        session.close();
        return true;
    }

    public void beginCall(Invocation current) {
        invocation = current;
        current.enter();
    }

    public void endCall() {
        if (invocation != null) invocation.leave();
        invocation = null;
    }

    private void setProperty() {
        bindHost();
        bindNetwork();
        bindCache();
        bindUtilities();
        bindSocketsAndDownloads();
    }

    private void bindHost() {
        ctx.bind("s2t", args -> s2t(string(args, 0))).join();
        ctx.bind("t2s", args -> t2s(string(args, 0))).join();
        ctx.bind("openWeb", args -> openWeb((QuickJS.Json) argument(args, 0))).join();
        ctx.bind("getCookie", args -> getCookie(string(args, 0))).join();
        ctx.bind("setCookie", args -> setCookie(string(args, 0), string(args, 1))).join();
        ctx.bind("getUserAgent", args -> getUserAgent()).join();
        ctx.bind("getPort", args -> getPort()).join();
        ctx.bind("getProxy", args -> getProxy((Boolean) argument(args, 0))).join();
        ctx.bind("proxyUrl", args -> proxyUrl((QuickJS.Json) argument(args, 0))).join();
        ctx.bind("getDanmakuUrl", args -> Spider.getDanmakuUrl(siteKey, string(args, 0))).join();
        ctx.bind("emitDanmaku", args -> emitDanmaku(integer(args, 0), (QuickJS.Json) argument(args, 1))).join();
        ctx.bind("js2Proxy", args -> js2Proxy((Boolean) argument(args, 0), integer(args, 1), string(args, 2), string(args, 3), (QuickJS.Json) argument(args, 4))).join();
    }

    private void bindNetwork() {
        ctx.bind("_netSession", args -> network.openSession()).join();
        ctx.bind("_netGetCookie", args -> network.getCookie(string(args, 0), integer(args, 1))).join();
        ctx.bind("_netSetCookie", args -> network.setCookie(string(args, 0), string(args, 1), integer(args, 2))).join();
        ctx.bind("_netClose", args -> closeSession(integer(args, 0))).join();
        ctx.bind("__http", args -> network.http(string(args, 0), (QuickJS.Json) argument(args, 1), (QuickJS.Callback) argument(args, 2))).join();
        ctx.bindAsync("_httpAsync", args -> network.requestAsync(string(args, 0), (QuickJS.Json) argument(args, 1)), this::captureInvocation).join();
        ctx.bindAsync("_stream", args -> streams.proxy(string(args, 0), (QuickJS.Json) argument(args, 1)), this::captureInvocation).join();
        ctx.bind("_streamBody", args -> streams.body((QuickJS.Callback) argument(args, 0), (QuickJS.Callback) argument(args, 1))).join();
        ctx.bindAsync("_openStream", args -> streams.open(string(args, 0), (QuickJS.Json) argument(args, 1), true), this::captureInvocation).join();
        ctx.bindAsync("_streamRead", args -> streams.read(string(args, 0), integer(args, 1)), this::captureInvocation).join();
        ctx.bind("_streamClose", args -> invocation != null && invocation.discardStream(string(args, 0))).join();
        ctx.bind("__ws", args -> network.webSocket(string(args, 0), (QuickJS.Json) argument(args, 1), (QuickJS.Callback) argument(args, 2))).join();
        ctx.bindAsync("_sleep", args -> network.sleep(((Number) argument(args, 0)).doubleValue(), integer(args, 1)), this::captureInvocation).join();
    }

    private void bindCache() {
        ctx.bind("_cached", args -> cache.load(string(args, 0), string(args, 1), integer(args, 2), (QuickJS.Callback) argument(args, 3), (QuickJS.Callback) argument(args, 4), (Boolean) argument(args, 5))).join();
        ctx.bind("_peek", args -> cache.peek(string(args, 0), integer(args, 1))).join();
        ctx.bind("_clearCache", args -> cache.clear(string(args, 0), integer(args, 1))).join();
        ctx.bind("_local", args -> cache.local(string(args, 0), string(args, 1), string(args, 2))).join();
        ctx.bind("_cacheResolve", args -> cache.resolve(integer(args, 0), string(args, 1), string(args, 2))).join();
    }

    private void bindUtilities() {
        ctx.bindAsync("evalJs", args -> evalJs(string(args, 0), (QuickJS.Json) argument(args, 1)), this::captureInvocation).join();
        ctx.bind("joinUrl", args -> joinUrl(string(args, 0), string(args, 1))).join();
        ctx.bind("htmlQuery", this::htmlQuery).join();
        ctx.bind("md5X", args -> md5X(string(args, 0))).join();
        ctx.bind("sha256X", args -> sha256X(string(args, 0))).join();
        ctx.bind("randomX", args -> randomX(integer(args, 0))).join();
        ctx.bind("aesX", args -> aesX(string(args, 0), (Boolean) argument(args, 1), string(args, 2), (Boolean) argument(args, 3), string(args, 4), string(args, 5), (Boolean) argument(args, 6))).join();
        ctx.bind("desX", args -> desX(string(args, 0), (Boolean) argument(args, 1), string(args, 2), (Boolean) argument(args, 3), string(args, 4), string(args, 5), (Boolean) argument(args, 6))).join();
        ctx.bind("rsaX", args -> rsaX(string(args, 0), (Boolean) argument(args, 1), (Boolean) argument(args, 2), string(args, 3), (Boolean) argument(args, 4), string(args, 5), (Boolean) argument(args, 6))).join();
    }

    private CompletableFuture<Object> evalJs(String source, QuickJS.Json options) {
        checkInvocation();
        CompletableFuture<String> evaluation = js.evalAsync(source, options == null ? "{}" : options.text());
        if (invocation != null) invocation.track(evaluation);
        CompletableFuture<Object> result = evaluation.thenApply(QuickJS.Json::new);
        result.whenComplete((value, error) -> {
            if (result.isCancelled()) evaluation.cancel(true);
        });
        return result;
    }

    private void bindSocketsAndDownloads() {
        ctx.bind("_connect", args -> sockets.connect(string(args, 0), (QuickJS.Json) argument(args, 1), (QuickJS.Callback) argument(args, 2))).join();
        ctx.bind("_socketSend", args -> sockets.send(integer(args, 0), string(args, 1), (byte[]) argument(args, 2))).join();
        ctx.bind("_socketClose", args -> {
            sockets.close(integer(args, 0), (Boolean) argument(args, 1));
            return null;
        }).join();
        ctx.bind("_download", args -> {
            network.download(string(args, 0), string(args, 1), (QuickJS.Json) argument(args, 2), (QuickJS.Callback) argument(args, 3));
            return null;
        }).join();
    }

    private QuickJS.Json htmlQuery(Object[] args) throws IOException {
        QuickJS.Json plan = (QuickJS.Json) argument(args, 1);
        Invocation current = invocation;
        long start = System.nanoTime();
        try {
            return new QuickJS.Json(HtmlQuery.query(string(args, 0), plan == null ? null : plan.text()));
        } finally {
            if (current != null) current.recordHtml(System.nanoTime() - start);
        }
    }

    boolean submit(Runnable runnable) {
        if (destroyed) return false;
        CompletableFuture<Void> future = ctx.execute(runnable);
        future.whenComplete((result, error) -> {
            if (error != null && !destroyed) SpiderDebug.log(error);
        });
        return !future.isCompletedExceptionally();
    }

    CompletableFuture<Object> invoke(Invocation current, QuickJS.Callback callback, Object... args) {
        CompletableFuture<Object> result = new CompletableFuture<>();
        if (destroyed) {
            result.completeExceptionally(new IllegalStateException("Spider destroyed"));
        } else {
            ctx.execute(() -> invoke(current, callback, result, args)).whenComplete((value, error) -> {
                if (error != null) result.completeExceptionally(error);
            });
        }
        return result;
    }

    private void invoke(Invocation current, QuickJS.Callback callback, CompletableFuture<Object> result, Object[] args) {
        if (result.isDone()) return;
        Invocation previous = invocation;
        invocation = current;
        current.enter();
        try {
            CompletableFuture<Object> source = callback.callDetached(args);
            result.whenComplete((value, error) -> {
                if (result.isCancelled()) source.cancel(false);
            });
            source.whenComplete((value, error) -> {
                if (error == null) result.complete(value);
                else result.completeExceptionally(error);
            });
        } catch (RuntimeException | Error error) {
            result.completeExceptionally(error);
        } finally {
            current.leave();
            invocation = previous;
        }
    }

    public int addDanmaku(Consumer<JSONObject> emit) {
        if (destroyed) throw new IllegalStateException("Spider destroyed");
        int id = nextDanmaku.incrementAndGet();
        danmakus.put(id, emit);
        return id;
    }

    public void removeDanmaku(int id) {
        danmakus.remove(id);
    }

    private Object emitDanmaku(int id, QuickJS.Json item) throws JSONException {
        Consumer<JSONObject> emit = danmakus.get(id);
        if (emit != null) emit.accept(new JSONObject(item.text()));
        return null;
    }

    public String s2t(String text) {
        return Trans.s2t(false, text);
    }

    public String t2s(String text) {
        return Trans.t2s(false, text);
    }

    public Boolean openWeb(QuickJS.Json options) {
        return options != null && host.openWeb(options.text());
    }

    public String getCookie(String url) {
        return host.getCookie(url);
    }

    public Boolean setCookie(String url, String cookie) {
        return host.setCookie(url, cookie);
    }

    public String getUserAgent() {
        return host.getUserAgent();
    }

    public Integer getPort() {
        return Proxy.getPort();
    }

    public String getProxy(Boolean local) {
        return Proxy.getUrl(local) + "?do=js";
    }

    public String proxyUrl(QuickJS.Json params) {
        Uri.Builder url = Uri.parse(Proxy.getUrl(true)).buildUpon().appendQueryParameter("do", "js").appendQueryParameter("siteKey", siteKey);
        if (params != null) {
            for (Map.Entry<String, JsonElement> entry : Json.strict(params.text()).getAsJsonObject().entrySet()) {
                if (entry.getKey().equals("do") || entry.getKey().equals("siteKey")) throw new IllegalArgumentException("Reserved proxy parameter");
                url.appendQueryParameter(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return url.build().toString();
    }

    public String js2Proxy(Boolean dynamic, Integer siteType, String siteKey, String url, QuickJS.Json headers) {
        return getProxy(!dynamic) + String.format("&from=catvod&siteType=%s&siteKey=%s&header=%s&url=%s", siteType, siteKey, Uri.encode(headers.text()), Uri.encode(url));
    }

    public String joinUrl(String parent, String child) {
        return UriUtil.resolve(parent, child);
    }

    public String md5X(String text) {
        return Crypto.md5(text);
    }

    public String sha256X(String text) {
        return Crypto.sha256(text);
    }

    public String randomX(Integer size) {
        return Crypto.randomUrlSafe(size == null ? 0 : size);
    }

    public String aesX(String mode, boolean encrypt, String input, boolean inBase64, String key, String iv, boolean outBase64) {
        return Crypto.aes(mode, encrypt, input, inBase64, key, iv, outBase64);
    }

    public String desX(String mode, boolean encrypt, String input, boolean inBase64, String key, String iv, boolean outBase64) {
        return Crypto.des(mode, encrypt, input, inBase64, key, iv, outBase64);
    }

    public String rsaX(String mode, boolean pub, boolean encrypt, String input, boolean inBase64, String key, boolean outBase64) {
        return Crypto.rsa(mode, pub, encrypt, input, inBase64, key, outBase64);
    }

    void complete(QuickJS.Callback complete, NetOptions req, Net.Result result) {
        submit(() -> invoke(complete, () -> complete.call(Connect.result(req, result))));
    }

    void complete(QuickJS.Callback complete, Object... args) {
        submit(() -> invoke(complete, () -> complete.call(args)));
    }

    void hold(QuickJS.Callback callback) {
        callbacks.put(callback, invocation);
    }

    void hold(QuickJS.Callback callback, Invocation current) {
        callbacks.put(callback, current);
    }

    Invocation current() {
        return invocation;
    }

    Invocation setInvocation(Invocation current) {
        Invocation previous = invocation;
        invocation = current;
        return previous;
    }

    boolean isDestroyed() {
        return destroyed;
    }

    void checkInvocation() {
        if (invocation != null && invocation.isCanceled()) throw new CancellationException("Spider call canceled");
    }

    private QuickJS.AsyncScope captureInvocation() {
        Invocation current = invocation;
        return () -> {
            Invocation previous = invocation;
            invocation = current;
            if (current != null) current.enter();
            return () -> {
                if (current != null) current.leave();
                invocation = previous;
            };
        };
    }

    void invoke(QuickJS.Callback callback, Supplier<CompletableFuture<Object>> callable) {
        Invocation previous = invocation;
        Invocation current = callbacks.get(callback);
        invocation = current;
        try {
            if (!destroyed) callable.get().whenComplete((result, error) -> {
                if (error == null) return;
                if (current != null) current.fail(error);
                SpiderDebug.log(error);
            });
        } finally {
            invocation = previous;
            release(callback);
        }
    }

    void release(QuickJS.Callback callback) {
        if (callbacks.containsKey(callback)) {
            callbacks.remove(callback);
            callback.close();
        }
    }
}
