package com.fongmi.quickjs.method;

import com.fongmi.quickjs.QuickJS;
import com.github.catvod.net.Net;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Local;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class Cache {

    private final Global global;
    private final Network network;
    private final Local local;
    private final AtomicInteger nextId = new AtomicInteger();
    private final Map<Integer, CompletableFuture<String>> loads = new ConcurrentHashMap<>();

    Cache(Global global, Network network, Local local) {
        this.global = global;
        this.network = network;
        this.local = local;
    }

    Boolean load(String key, String options, Integer id, QuickJS.Callback loader, QuickJS.Callback complete, Boolean background) {
        if (loader == null || complete == null || global.isDestroyed()) {
            if (loader != null) loader.close();
            if (complete != null) complete.close();
            return false;
        }
        holdCallbacks(loader, complete, background);
        AtomicInteger ticket = new AtomicInteger();
        try {
            CompletableFuture<String> result = request(key, options, id, ticket);
            Invocation current = global.current();
            if (!Boolean.TRUE.equals(background) && current != null) current.track(result);
            result.whenComplete((json, error) -> global.complete(complete, json, Global.errorMessage(error)));
            startLoader(loader, ticket.get());
        } catch (Exception e) {
            global.release(loader);
            global.complete(complete, null, Global.errorMessage(e));
        }
        return true;
    }

    private void holdCallbacks(QuickJS.Callback loader, QuickJS.Callback complete, Boolean background) {
        global.hold(complete, Boolean.TRUE.equals(background) ? null : global.current());
        global.hold(loader, null);
    }

    private CompletableFuture<String> request(String key, String options, Integer id, AtomicInteger ticket) {
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("Invalid cache key");
        Net client = network.client(id);
        return client.cachedAsync(key, options, () -> createLoad(ticket));
    }

    private CompletableFuture<String> createLoad(AtomicInteger ticket) {
        int id = nextId.incrementAndGet();
        CompletableFuture<String> load = new CompletableFuture<>();
        loads.put(id, load);
        load.whenComplete((json, error) -> loads.remove(id, load));
        ticket.set(id);
        return load;
    }

    private void startLoader(QuickJS.Callback loader, int id) {
        if (id == 0) {
            global.release(loader);
            return;
        }
        Invocation loading = new Invocation();
        CompletableFuture<String> result = loads.get(id);
        if (result != null) loading.bind(result);
        global.hold(loader, loading);
        global.submit(() -> global.invoke(loader, () -> invokeLoader(loader, id, loading)));
    }

    private CompletableFuture<Object> invokeLoader(QuickJS.Callback loader, int id, Invocation loading) {
        if (!loads.containsKey(id)) return CompletableFuture.completedFuture(null);
        CompletableFuture<Object> delivery = loader.callDetached(id);
        loading.track(delivery);
        return delivery.whenComplete((value, error) -> loading.close());
    }

    Boolean resolve(Integer ticket, String json, String error) {
        CompletableFuture<String> load = ticket == null ? null : loads.remove(ticket);
        if (load == null) return false;
        return error == null ? load.complete(json) : load.completeExceptionally(new IllegalStateException(error));
    }

    String peek(String key, Integer id) {
        JsonObject result = new JsonObject();
        try {
            String value = network.client(id).peek(key);
            return value == null ? "{\"found\":false}" : "{\"found\":true,\"value\":" + value + "}";
        } catch (Exception e) {
            result.addProperty("error", Net.Result.failure(e).error);
        }
        return result.toString();
    }

    String clear(String key, Integer id) {
        try {
            network.client(id).clearCache(key);
            return "";
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    String local(String operation, String key, String value) {
        JsonObject result = new JsonObject();
        try {
            switch (operation) {
                case "get" -> readLocal(result, key);
                case "set" -> local.set(key, value);
                case "delete" -> local.delete(key);
                default -> throw new IllegalArgumentException("Invalid Local operation");
            }
        } catch (Exception e) {
            result.addProperty("error", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
        return result.toString();
    }

    private void readLocal(JsonObject result, String key) {
        String saved = local.get(key);
        result.addProperty("found", saved != null);
        if (saved != null) result.add("value", Json.strict(saved));
    }

    void release() {
        loads.clear();
    }
}
