package com.fongmi.quickjs.method;

import android.net.Uri;

import com.fongmi.quickjs.QuickJS;
import com.fongmi.quickjs.WebPlatform;
import com.fongmi.quickjs.host.Host;
import com.fongmi.quickjs.utils.HtmlQuery;
import com.github.catvod.Proxy;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.Net;
import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Js;
import com.github.catvod.utils.Local;
import com.github.catvod.utils.Task;
import com.github.catvod.utils.Trans;
import com.github.catvod.utils.UriUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    private final Streams streams;
    private final Sockets sockets;
    private final Dialogs dialogs;
    private final Cache cache;
    private final QuickJS ctx;
    private final QuickJS.Reference bridge;
    private final Js js = new Js();
    private final Host host;
    private final String siteKey;
    private final AtomicInteger nextDanmaku = new AtomicInteger();
    private final Map<QuickJS.Callback, Invocation> callbacks = new HashMap<>();
    private final Map<Integer, Consumer<JSONObject>> danmakus = new ConcurrentHashMap<>();

    private QuickJS.Reference dialogActions;
    private QuickJS.Reference spider;
    private Invocation invocation;

    private volatile boolean destroyed;

    private Global(QuickJS ctx, Host host, String siteKey, String api) {
        this.ctx = ctx;
        this.host = host;
        this.siteKey = siteKey;
        this.bridge = (QuickJS.Reference) ctx.reference("newObject").join();
        this.web = new WebPlatform(ctx, SpiderDebug::log, api);
        this.network = new Network(this, new Net(host, siteKey), web);
        this.cache = new Cache(this, network, new Local(siteKey));
        this.streams = new Streams(this, network, host.getCallTimeout());
        this.sockets = new Sockets(this, network);
        this.dialogs = new Dialogs(this, host);
        ctx.reference("set", bridge, "siteKey", siteKey).join();
        ctx.setAsyncScopeCapture(this::captureScope).join();
        ctx.registerModule("catvod:host", bridge).join();
        ctx.onClose(this::destroy).join();
        bind();
    }

    public static Global create(QuickJS ctx, Host host, String siteKey, String api) {
        return new Global(ctx, host, siteKey, api);
    }

    private static Object arg(Object[] args, int index) {
        return index < args.length ? args[index] : null;
    }

    private static String str(Object[] args, int index) {
        return (String) arg(args, index);
    }

    private static Integer integer(Object[] args, int index) {
        Number value = (Number) arg(args, index);
        return value == null ? null : value.intValue();
    }

    private static QuickJS.Json json(Object[] args, int index) {
        return (QuickJS.Json) arg(args, index);
    }

    private static QuickJS.Callback callback(Object[] args, int index) {
        return (QuickJS.Callback) arg(args, index);
    }

    private static Boolean bool(Object[] args, int index) {
        return (Boolean) arg(args, index);
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
        dialogs.close();
        js.close();
        cache.release();
        network.release();
        danmakus.clear();
        releaseCallbacks();
        sockets.release();
        web.close();
        releaseReferences();
    }

    private void releaseReferences() {
        if (spider != null) spider.close();
        if (dialogActions != null) dialogActions.close();
        bridge.close();
    }

    private void releaseCallbacks() {
        for (QuickJS.Callback callback : callbacks.keySet()) callback.close();
        callbacks.clear();
    }

    public void cancelRequests() {
        dialogs.close();
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

    public CompletableFuture<Object> call(String method, Consumer<CompletableFuture<Object>> before, Runnable after, Object... args) {
        return ctx.invoke(spider, method, before, after, args);
    }

    private Object attach(QuickJS.Reference value, QuickJS.Reference actions) {
        spider = value;
        dialogActions = actions;
        return null;
    }

    private void bind() {
        ctx.bindReference(bridge, "attach", args -> attach((QuickJS.Reference) arg(args, 0), (QuickJS.Reference) arg(args, 1))).join();
        bindHost();
        bindNet();
        bindSocket();
        bindCache();
        bindUtils();
    }

    private void bindHost() {
        ctx.bind(bridge, "openWeb", args -> openWeb(json(args, 0))).join();
        ctx.bindAsync(bridge, "dialog", args -> dialogs.open(json(args, 0), str(args, 1)), this::captureScope).join();
        ctx.bind(bridge, "getPort", args -> getPort()).join();
        ctx.bind(bridge, "proxyUrl", args -> proxyUrl(json(args, 0), bool(args, 1))).join();
        ctx.bind(bridge, "danmakuUrl", args -> Spider.danmakuUrl(siteKey, str(args, 0))).join();
        ctx.bind(bridge, "emitDanmaku", args -> emitDanmaku(integer(args, 0), json(args, 1))).join();
    }

    private void bindNet() {
        ctx.bind(bridge, "getUserAgent", args -> host.getUserAgent()).join();
        ctx.bind(bridge, "getCookie", args -> getCookie(str(args, 0), integer(args, 1))).join();
        ctx.bind(bridge, "setCookie", args -> setCookie(str(args, 0), str(args, 1), integer(args, 2))).join();
        ctx.bind(bridge, "session", args -> network.openSession()).join();
        ctx.bind(bridge, "closeSession", args -> closeSession(integer(args, 0))).join();
        ctx.bindAsync(bridge, "req", args -> network.req(str(args, 0), json(args, 1)), this::captureScope).join();
        ctx.bindAsync(bridge, "ws", args -> network.ws(str(args, 0), json(args, 1)), this::captureScope).join();
        ctx.bind(bridge, "download", args -> download(str(args, 0), str(args, 1), json(args, 2), callback(args, 3))).join();
        ctx.bindAsync(bridge, "sleep", args -> network.sleep(((Number) arg(args, 0)).doubleValue(), integer(args, 1)), this::captureScope).join();
        ctx.bindAsync(bridge, "stream", args -> streams.proxy(str(args, 0), json(args, 1)), this::captureScope).join();
        ctx.bind(bridge, "streamBody", args -> streams.body(callback(args, 0), callback(args, 1))).join();
        ctx.bindAsync(bridge, "openStream", args -> streams.open(str(args, 0), json(args, 1), true), this::captureScope).join();
        ctx.bindAsync(bridge, "streamRead", args -> streams.read(str(args, 0), integer(args, 1)), this::captureScope).join();
        ctx.bind(bridge, "streamClose", args -> closeStream(str(args, 0))).join();
    }

    private void bindCache() {
        ctx.bind(bridge, "cached", args -> cache.load(str(args, 0), str(args, 1), integer(args, 2), callback(args, 3), callback(args, 4), bool(args, 5))).join();
        ctx.bind(bridge, "peek", args -> cache.peek(str(args, 0), integer(args, 1))).join();
        ctx.bind(bridge, "clearCache", args -> cache.clear(str(args, 0), integer(args, 1))).join();
        ctx.bind(bridge, "cacheResolve", args -> cache.resolve(integer(args, 0), str(args, 1), str(args, 2))).join();
        ctx.bind(bridge, "local", args -> cache.local(str(args, 0), str(args, 1), str(args, 2))).join();
    }

    private void bindUtils() {
        ctx.bind(bridge, "s2t", args -> s2t(str(args, 0))).join();
        ctx.bind(bridge, "t2s", args -> t2s(str(args, 0))).join();
        ctx.bind(bridge, "joinUrl", args -> joinUrl(str(args, 0), str(args, 1))).join();
        ctx.bind(bridge, "htmlQuery", args -> htmlQuery(str(args, 0), json(args, 1))).join();
        ctx.bindAsync(bridge, "eval", args -> eval(str(args, 0), json(args, 1)), this::captureScope).join();
        ctx.bind(bridge, "md5X", args -> md5X(str(args, 0))).join();
        ctx.bind(bridge, "sha256X", args -> sha256X(str(args, 0))).join();
        ctx.bind(bridge, "randomX", args -> randomX(integer(args, 0))).join();
        ctx.bind(bridge, "aesX", args -> aesX(str(args, 0), bool(args, 1), str(args, 2), bool(args, 3), str(args, 4), str(args, 5), bool(args, 6))).join();
        ctx.bind(bridge, "desX", args -> desX(str(args, 0), bool(args, 1), str(args, 2), bool(args, 3), str(args, 4), str(args, 5), bool(args, 6))).join();
        ctx.bind(bridge, "rsaX", args -> rsaX(str(args, 0), bool(args, 1), bool(args, 2), str(args, 3), bool(args, 4), str(args, 5), bool(args, 6))).join();
    }

    private void bindSocket() {
        ctx.bind(bridge, "connect", args -> sockets.connect(str(args, 0), json(args, 1), callback(args, 2))).join();
        ctx.bind(bridge, "socketSend", args -> sockets.send(integer(args, 0), str(args, 1), (byte[]) arg(args, 2))).join();
        ctx.bind(bridge, "socketClose", args -> closeSocket(integer(args, 0), bool(args, 1))).join();
    }

    private CompletableFuture<Object> eval(String source, QuickJS.Json options) {
        checkInvocation();
        CompletableFuture<String> evaluation = js.evalAsync(source, options == null ? "{}" : options.text());
        if (invocation != null) invocation.track(evaluation);
        CompletableFuture<Object> result = evaluation.thenApply(QuickJS.Json::new);
        result.whenComplete((value, error) -> {
            if (result.isCancelled()) evaluation.cancel(true);
        });
        return result;
    }

    private String getCookie(String url, Integer id) {
        return id == null ? host.getCookie(url) : network.getCookie(url, id);
    }

    private Boolean setCookie(String url, String cookie, Integer id) {
        return id == null ? host.setCookie(url, cookie) : network.setCookie(url, cookie, id);
    }

    private Object closeSocket(Integer id, Boolean cancel) {
        sockets.close(id, cancel);
        return null;
    }

    private Boolean closeStream(String token) {
        return invocation != null && invocation.discardStream(token);
    }

    private Object download(String url, String path, QuickJS.Json options, QuickJS.Callback complete) {
        network.download(url, path, options, complete);
        return null;
    }

    private QuickJS.Json htmlQuery(String html, QuickJS.Json plan) throws IOException {
        Invocation current = invocation;
        long start = System.nanoTime();
        try {
            return new QuickJS.Json(HtmlQuery.query(html, plan == null ? null : plan.text()));
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
        if (destroyed) return Task.failedFuture(new IllegalStateException("Spider destroyed"));
        CompletableFuture<Object> result = new CompletableFuture<>();
        ctx.execute(() -> invoke(current, callback, result, args)).whenComplete((value, error) -> {
            if (error != null) result.completeExceptionally(error);
        });
        return result;
    }

    CompletableFuture<Object> invokeDialogAction(Invocation current, String id, String event, Consumer<CompletableFuture<Object>> started) {
        Invocation[] previous = new Invocation[1];
        return ctx.invoke(dialogActions, "action", future -> {
            previous[0] = invocation;
            invocation = current;
            if (current != null) {
                current.enter();
                current.track(future);
            }
            started.accept(future);
        }, () -> {
            if (current != null) current.leave();
            invocation = previous[0];
        }, id, new QuickJS.Json(event));
    }

    void releaseDialogAction(String id) {
        submit(() -> ctx.invoke(dialogActions, "release", id).whenComplete((value, error) -> {
            if (error != null && !destroyed) SpiderDebug.log(error);
        }));
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

    public Integer getPort() {
        return Proxy.getPort();
    }

    private String proxyUrl(QuickJS.Json params, boolean local) {
        Uri.Builder url = Uri.parse(Proxy.getUrl(local)).buildUpon();
        url.appendQueryParameter("do", "js");
        url.appendQueryParameter("siteKey", siteKey);
        JsonObject values = JsonParser.parseString(params.text()).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : values.entrySet()) url.appendQueryParameter(entry.getKey(), entry.getValue().getAsString());
        return url.build().toString();
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

    private QuickJS.AsyncScope captureScope() {
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
