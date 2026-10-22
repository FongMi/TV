package com.fongmi.quickjs.method;

import com.fongmi.quickjs.QuickJS;
import com.github.catvod.net.Net;
import com.github.catvod.net.NetOptions;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class Streams {

    private static final Gson GSON = new Gson();

    private final Global global;
    private final Network network;
    private final AtomicInteger streamId = new AtomicInteger();
    private final String streamPrefix = UUID.randomUUID().toString() + ":";
    private final Map<String, ProxyBody> bodies = new ConcurrentHashMap<>();
    private final long timeout;

    Streams(Global global, Network network, long timeout) {
        this.global = global;
        this.network = network;
        this.timeout = timeout;
    }

    QuickJS.Json body(QuickJS.Callback pull, QuickJS.Callback cancel) {
        Invocation current = global.current();
        String token = streamPrefix + streamId.incrementAndGet();
        ProxyBody body = new ProxyBody(global, pull, cancel, () -> bodies.remove(token), timeout);
        try {
            global.checkInvocation();
            if (current == null || !current.acceptsStream(false)) throw new IllegalStateException("Stream bodies require a proxy invocation");
            bodies.put(token, body);
            if (!current.holdStream(token, body, false)) throw new IllegalStateException("Proxy invocation ended");
            return new QuickJS.Json(GSON.toJson(Map.of("token", token)));
        } catch (RuntimeException error) {
            body.close();
            throw error;
        }
    }

    void close() {
        Throwable failure = null;
        for (ProxyBody body : new ArrayList<>(bodies.values())) {
            try {
                body.close();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private static void completeRead(Invocation current, String token, ByteBuffer bytes, Integer count, Throwable error, CompletableFuture<Object> result) {
        if (error != null) {
            current.discardStream(token);
            result.completeExceptionally(error);
        } else if (count == -1) {
            current.discardStream(token);
            result.complete(null);
        } else {
            bytes.flip();
            result.complete(new QuickJS.DirectBuffer(bytes));
        }
    }

    CompletableFuture<Object> proxy(String url, QuickJS.Json options) {
        return open(url, options, false);
    }

    CompletableFuture<Object> open(String url, QuickJS.Json options, boolean reader) {
        Invocation current = global.current();
        try {
            global.checkInvocation();
            if (current == null || !current.acceptsStream(reader)) throw new IllegalStateException("Stream invocation is unavailable");
            JsonObject json = Network.parseOptions(options);
            CompletableFuture<Net.StreamResult> source = network.client(json).requestStreamAsync(url, GSON.fromJson(json, NetOptions.class));
            current.trackNetwork(source);
            return completeStream(current, source, reader);
        } catch (Exception error) {
            return CompletableFuture.completedFuture(Invocation.streamError(error));
        }
    }

    private CompletableFuture<Object> completeStream(Invocation current, CompletableFuture<Net.StreamResult> source, boolean reader) {
        CompletableFuture<Object> result = new CompletableFuture<>();
        result.whenComplete((value, error) -> {
            if (result.isCancelled()) source.cancel(true);
        });
        source.whenComplete((response, error) -> {
            if (error != null) {
                result.complete(Invocation.streamError(error));
                return;
            }
            String token = streamPrefix + streamId.incrementAndGet();
            current.completeStream(token, response, reader, result);
        });
        return result;
    }

    CompletableFuture<Object> read(String token, Integer size) {
        global.checkInvocation();
        if (size == null || size < 1 || size > 1024 * 1024) throw new IllegalArgumentException("Chunk size must be 1..1048576 bytes");
        Invocation current = global.current();
        if (current == null) throw new IllegalStateException("Stream invocation is unavailable");
        Net.StreamResult response = current.reader(token);
        ByteBuffer bytes = ByteBuffer.allocateDirect(size);
        CompletableFuture<Integer> source = response.readAsync(bytes);
        CompletableFuture<Object> result = new CompletableFuture<>();
        current.trackNetwork(result);
        result.whenComplete((value, error) -> {
            if (result.isCancelled()) {
                source.cancel(true);
                current.discardStream(token);
            }
        });
        source.whenComplete((count, error) -> completeRead(current, token, bytes, count, error, result));
        return result;
    }
}
