package com.fongmi.quickjs.method;

import com.fongmi.quickjs.QuickJS;
import com.github.catvod.net.Net;
import com.github.catvod.utils.CallTimeout;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

public final class Invocation implements AutoCloseable {

    private static final Gson GSON = new Gson();

    private final Set<CompletableFuture<?>> pending = ConcurrentHashMap.newKeySet();
    private final Set<CompletableFuture<?>> dialogs = ConcurrentHashMap.newKeySet();
    private final Map<String, InputStream> streams = new HashMap<>();
    private final Set<String> readers = new HashSet<>();
    private final boolean proxy;
    private final long started = System.nanoTime();
    private final Map<CompletableFuture<?>, Long> networking = new HashMap<>();
    private volatile CompletableFuture<?> result;
    private CallTimeout timeout;
    private Runnable resumeDeadline;
    private long timeoutMillis;
    private volatile boolean canceled;
    private volatile boolean ended;
    private long firstTurn, ownerStart, ownerNanos, networkNanos, htmlNanos;
    private int depth, networkOperations, dialogActions;

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

    private static void closeStreams(List<InputStream> streams) {
        Throwable failure = null;
        for (InputStream stream : streams) {
            try {
                stream.close();
            } catch (IOException error) {
                if (failure == null) failure = new UncheckedIOException(error);
                else failure.addSuppressed(error);
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
            if (timeout != null) timeout.close();
            if (error != null) cancel();
        });
        if (timeout != null) timeout.start(() -> fail(new TimeoutException("JavaScript call exceeded " + timeoutMillis + " ms")));
        if (canceled) result.cancel(false);
    }

    public void timeout(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        timeout = new CallTimeout(timeoutMillis);
    }

    synchronized void await(CompletableFuture<?> future) {
        dialogs.add(future);
        future.whenComplete((value, error) -> endDialog(future));
        track(future);
        pauseTimeout();
        if (ended) future.cancel(true);
    }

    private synchronized void endDialog(CompletableFuture<?> future) {
        dialogs.remove(future);
        if (dialogs.isEmpty()) resumeTimeout();
    }

    private void pauseTimeout() {
        if (timeout != null && resumeDeadline == null && depth == 0 && dialogActions == 0 && !dialogs.isEmpty() && !ended) resumeDeadline = timeout.suspend();
    }

    synchronized Runnable beginDialogAction() {
        if (ended || canceled) throw new CancellationException("Spider call ended");
        dialogActions++;
        resumeTimeout();
        return this::endDialogAction;
    }

    private synchronized void endDialogAction() {
        dialogActions--;
        pauseTimeout();
    }

    private synchronized void resumeTimeout() {
        Runnable resume = resumeDeadline;
        resumeDeadline = null;
        if (resume != null) resume.run();
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
        if (depth++ == 0) {
            ownerStart = now;
            resumeTimeout();
        }
    }

    synchronized void leave() {
        if (--depth == 0) ownerNanos += System.nanoTime() - ownerStart;
        pauseTimeout();
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

    boolean holdStream(String token, InputStream stream, boolean reader) {
        synchronized (streams) {
            if (ended || canceled) return false;
            streams.put(token, stream);
            if (reader) readers.add(token);
            return true;
        }
    }

    public InputStream takeStream(String token) {
        synchronized (streams) {
            if (ended || canceled) throw new CancellationException("Proxy invocation ended");
            if (readers.contains(token)) throw new IllegalArgumentException("Chunk reader cannot be returned as a proxy body");
            InputStream stream = streams.remove(token);
            if (stream == null) throw new IllegalArgumentException("Unknown or consumed proxy stream");
            return stream;
        }
    }

    Net.StreamResult reader(String token) {
        synchronized (streams) {
            if (ended || canceled) throw new CancellationException("Stream invocation ended");
            if (!readers.contains(token)) throw new IllegalArgumentException("Unknown or closed chunk reader");
            return (Net.StreamResult) streams.get(token);
        }
    }

    boolean discardStream(String token) {
        InputStream stream;
        synchronized (streams) {
            stream = streams.remove(token);
            readers.remove(token);
        }
        if (stream != null) closeStreams(List.of(stream));
        return stream != null;
    }

    @Override
    public void close() {
        if (timeout != null) timeout.close();
        resumeTimeout();
        List<InputStream> unclaimed;
        synchronized (streams) {
            ended = true;
            unclaimed = new ArrayList<>(streams.values());
            streams.clear();
            readers.clear();
        }
        for (CompletableFuture<?> dialog : dialogs) dialog.cancel(true);
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
