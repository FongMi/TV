package com.fongmi.quickjs.method;

import com.fongmi.quickjs.QuickJS;
import com.github.catvod.net.Net;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class Invocation implements AutoCloseable {

    private static final Gson GSON = new Gson();

    private final Set<CompletableFuture<?>> pending = ConcurrentHashMap.newKeySet();
    private final Map<String, Net.StreamResult> streams = new HashMap<>();
    private final Set<String> readers = new HashSet<>();
    private final boolean proxy;
    private final long started = System.nanoTime();
    private final Map<CompletableFuture<?>, Long> networking = new HashMap<>();
    private volatile CompletableFuture<?> result;
    private volatile boolean canceled;
    private volatile boolean ended;
    private long firstTurn, ownerStart, ownerNanos, networkNanos, htmlNanos;
    private int depth, networkOperations;

    public Invocation() {
        this(false);
    }

    public Invocation(boolean proxy) {
        this.proxy = proxy;
    }

    boolean isCanceled() {
        return canceled;
    }

    boolean acceptsStream(boolean reader) {
        return !ended && (reader || proxy);
    }

    static QuickJS.Json streamError(Throwable error) {
        return new QuickJS.Json(GSON.toJson(Map.of("error", Global.errorMessage(error))));
    }

    private static void closeStreams(List<Net.StreamResult> streams) {
        Throwable failure = null;
        for (Net.StreamResult stream : streams) {
            try {
                stream.close();
            } catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    private static QuickJS.Json streamMetadata(String token, Net.StreamResult response) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("code", response.code);
        metadata.put("headers", response.headers);
        metadata.put("contentType", response.contentType);
        metadata.put("token", token);
        return new QuickJS.Json(GSON.toJson(metadata));
    }

    public void bind(CompletableFuture<?> result) {
        this.result = result;
        result.whenComplete((value, error) -> {
            if (error != null) cancel();
        });
        if (canceled) result.cancel(false);
    }

    void track(CompletableFuture<?> request) {
        pending.add(request);
        request.whenComplete((value, error) -> pending.remove(request));
        if (canceled) request.cancel(true);
    }

    void trackNetwork(CompletableFuture<?> request) {
        synchronized (this) {
            networkOperations++;
            networking.put(request, System.nanoTime());
        }
        request.whenComplete((value, error) -> {
            synchronized (this) {
                networkNanos += System.nanoTime() - networking.remove(request);
            }
        });
        track(request);
    }

    synchronized void enter() {
        long now = System.nanoTime();
        if (firstTurn == 0) firstTurn = now;
        if (depth++ == 0) ownerStart = now;
    }

    synchronized void leave() {
        if (--depth == 0) ownerNanos += System.nanoTime() - ownerStart;
    }

    synchronized void recordHtml(long nanos) {
        htmlNanos += nanos;
    }

    public synchronized Metrics metrics() {
        long now = System.nanoTime(), network = networkNanos;
        for (long begin : networking.values()) network += now - begin;
        synchronized (streams) {
            return new Metrics(now - started, (firstTurn == 0 ? now : firstTurn) - started,
                    ownerNanos + (depth == 0 ? 0 : now - ownerStart), network, htmlNanos, networkOperations,
                    (int) pending.stream().filter(request -> !request.isDone()).count(), streams.size());
        }
    }

    public void cancel() {
        canceled = true;
        try {
            close();
        } finally {
            cancelPending();
        }
    }

    private void cancelPending() {
        if (result != null) result.cancel(false);
        for (CompletableFuture<?> request : pending) request.cancel(true);
    }

    private boolean holdStream(String token, Net.StreamResult stream, boolean reader) {
        synchronized (streams) {
            if (ended || canceled) return false;
            streams.put(token, stream);
            if (reader) readers.add(token);
            return true;
        }
    }

    public Net.StreamResult takeStream(String token) {
        synchronized (streams) {
            if (ended || canceled) throw new CancellationException("Proxy invocation ended");
            if (readers.contains(token)) throw new IllegalArgumentException("Chunk reader cannot be returned as a proxy body");
            Net.StreamResult stream = streams.remove(token);
            if (stream == null) throw new IllegalArgumentException("Unknown or consumed proxy stream");
            return stream;
        }
    }

    Net.StreamResult reader(String token) {
        synchronized (streams) {
            if (ended || canceled) throw new CancellationException("Stream invocation ended");
            if (!readers.contains(token)) throw new IllegalArgumentException("Unknown or closed chunk reader");
            return streams.get(token);
        }
    }

    boolean discardStream(String token) {
        Net.StreamResult stream;
        synchronized (streams) {
            stream = streams.remove(token);
            readers.remove(token);
        }
        if (stream != null) stream.close();
        return stream != null;
    }

    @Override
    public void close() {
        List<Net.StreamResult> unclaimed;
        synchronized (streams) {
            ended = true;
            unclaimed = new ArrayList<>(streams.values());
            streams.clear();
            readers.clear();
        }
        closeStreams(unclaimed);
    }

    void fail(Throwable error) {
        if (result != null) result.completeExceptionally(error);
        cancel();
    }

    void completeStream(String token, Net.StreamResult response, boolean reader, CompletableFuture<Object> result) {
        try {
            if (!holdStream(token, response, reader)) {
                response.close();
                result.complete(streamError(new CancellationException("Proxy invocation ended")));
                return;
            }
            if (!result.complete(streamMetadata(token, response))) discardStream(token);
        } catch (Throwable failure) {
            discardFailedStream(token, failure);
            result.completeExceptionally(failure);
        }
    }

    private void discardFailedStream(String token, Throwable failure) {
        try {
            discardStream(token);
        } catch (Throwable closing) {
            if (closing != failure) failure.addSuppressed(closing);
        }
    }

    public record Metrics(long elapsedNanos, long queuedNanos, long ownerNanos, long networkNanos, long htmlNanos, int networkOperations, int pending, int streams) {
    }
}
