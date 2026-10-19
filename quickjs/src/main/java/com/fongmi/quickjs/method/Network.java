package com.fongmi.quickjs.method;

import com.fongmi.quickjs.QuickJS;
import com.fongmi.quickjs.WebPlatform;
import com.fongmi.quickjs.utils.Connect;
import com.github.catvod.net.Net;
import com.github.catvod.net.NetOptions;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class Network {

    private static final Gson GSON = new Gson();

    private final Global global;
    private final Net net;
    private final WebPlatform web;
    private final AtomicInteger sessionId = new AtomicInteger();
    private final Map<Integer, Net> sessions = new ConcurrentHashMap<>();
    private final Map<CompletableFuture<Void>, Net> sleeps = new HashMap<>();
    private boolean requestsCanceled;

    Network(Global global, Net net, WebPlatform web) {
        this.global = global;
        this.net = net;
        this.web = web;
    }

    static JsonObject parseOptions(QuickJS.Json options) {
        return JsonParser.parseString(options == null ? "{}" : options.text()).getAsJsonObject();
    }

    Net client(Integer id) {
        return id == null ? net : sessionNet(id);
    }

    Net client(JsonObject options) {
        JsonElement value = options.get("__net");
        if (value == null) return net;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Invalid network session");
        int id = value.getAsInt();
        if (id <= 0 || value.getAsDouble() != id) throw new IllegalArgumentException("Invalid network session");
        return sessionNet(id);
    }

    Integer openSession() {
        if (global.isDestroyed()) throw new IllegalStateException("Network owner is closed");
        Net session = net.session();
        int id = sessionId.incrementAndGet();
        sessions.put(id, session);
        return id;
    }

    String getCookie(String url, Integer id) {
        return sessionNet(id).getCookie(url);
    }

    Boolean setCookie(String url, String cookie, Integer id) {
        return sessionNet(id).setCookie(url, cookie);
    }

    Net removeSession(Integer id) {
        Net session = id == null ? null : sessions.remove(id);
        if (session == null) throw new IllegalStateException("Network session is unavailable");
        return session;
    }

    private Net sessionNet(Integer id) {
        Net session = id == null ? null : sessions.get(id);
        if (session == null) throw new IllegalStateException("Network session is unavailable");
        return session;
    }

    Object http(String url, QuickJS.Json options, QuickJS.Callback complete) {
        if (complete == null) return request(url, options);
        requestAsync(url, options, complete, false);
        return null;
    }

    Object webSocket(String url, QuickJS.Json options, QuickJS.Callback complete) {
        if (complete == null) return null;
        requestAsync(url, options, complete, true);
        return null;
    }

    private Object request(String url, QuickJS.Json options) {
        try {
            global.checkInvocation();
            JsonObject json = parseOptions(options);
            NetOptions req = GSON.fromJson(json, NetOptions.class);
            return Connect.result(req, sendRequest(url, json, req, false).get());
        } catch (Exception e) {
            return Connect.error(e);
        }
    }

    CompletableFuture<Object> requestAsync(String url, QuickJS.Json options) {
        try {
            global.checkInvocation();
            JsonObject json = parseOptions(options);
            NetOptions req = GSON.fromJson(json, NetOptions.class);
            return completeResponse(req, sendRequest(url, json, req, false));
        } catch (Exception error) {
            return CompletableFuture.completedFuture(Connect.error(error));
        }
    }

    private void requestAsync(String url, QuickJS.Json options, QuickJS.Callback complete, boolean socket) {
        global.hold(complete);
        try {
            global.checkInvocation();
            JsonObject json = parseOptions(options);
            NetOptions req = GSON.fromJson(json, NetOptions.class);
            sendRequest(url, json, req, socket).whenComplete((response, error) -> global.complete(complete, req, error == null ? response : Net.Result.failure(error)));
        } catch (Exception e) {
            global.complete(complete, new NetOptions(), Net.Result.failure(e));
        }
    }

    private CompletableFuture<Net.Result> sendRequest(String url, JsonObject json, NetOptions options, boolean socket) {
        Net client = client(json);
        CompletableFuture<Net.Result> result = socket ? client.socketAsync(url, options) : client.requestAsync(url, options);
        Invocation current = global.current();
        if (current != null) current.trackNetwork(result);
        return result;
    }

    private static CompletableFuture<Object> completeResponse(NetOptions req, CompletableFuture<Net.Result> source) {
        CompletableFuture<Object> result = source.handle((response, error) -> Connect.result(req, error == null ? response : Net.Result.failure(error)));
        result.whenComplete((value, error) -> {
            if (result.isCancelled()) source.cancel(true);
        });
        return result;
    }

    void download(String url, String path, QuickJS.Json options, QuickJS.Callback complete) {
        global.hold(complete);
        try {
            global.checkInvocation();
            JsonObject json = parseOptions(options);
            CompletableFuture<String> result = client(json).downloadAsync(url, json.toString(), new File(path));
            Invocation current = global.current();
            if (current != null) current.track(result);
            result.whenComplete((value, error) -> global.complete(complete, Global.errorMessage(error)));
        } catch (Exception e) {
            global.complete(complete, Global.errorMessage(e));
        }
    }

    CompletableFuture<Void> sleep(double milliseconds, Integer id) {
        global.checkInvocation();
        long duration = duration(milliseconds, "Invalid sleep duration");
        Net owner = id == null ? net : sessionNet(id);
        CompletableFuture<Void> result = scheduleSleep(owner, duration);
        result.whenComplete((value, error) -> releaseSleep(result));
        Invocation current = global.current();
        if (current != null) current.track(result);
        return result;
    }

    private long duration(double value, String message) {
        if (!Double.isFinite(value) || value < 0 || value > 9007199254740991d || value != (long) value) throw new IllegalArgumentException(message);
        return (long) value;
    }

    private CompletableFuture<Void> scheduleSleep(Net owner, long duration) {
        synchronized (sleeps) {
            if (global.isDestroyed() || requestsCanceled) throw new CancellationException("Network owner closed");
            CompletableFuture<Void> result = web.delay(duration);
            sleeps.put(result, owner);
            return result;
        }
    }

    private void releaseSleep(CompletableFuture<Void> result) {
        synchronized (sleeps) {
            sleeps.remove(result);
        }
    }

    void cancelSleeps(Net owner) {
        List<CompletableFuture<Void>> pending;
        synchronized (sleeps) {
            if (owner == net) requestsCanceled = true;
            pending = sleeps.entrySet().stream().filter(entry -> owner == net || entry.getValue() == owner).map(Map.Entry::getKey).toList();
        }
        pending.forEach(result -> result.cancel(false));
    }

    void cancelSleeps() {
        cancelSleeps(net);
    }

    void close() {
        net.close();
    }

    void release() {
        sessions.clear();
    }
}
