package com.github.catvod.net;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.EventListener;
import okhttp3.Response;

public final class NetTrace implements AutoCloseable {

    private static final int LIMIT = 100;
    private static final ConcurrentHashMap<String, NetTrace> owners = new ConcurrentHashMap<>();
    private final String key;
    private final List<Entry> entries = new ArrayList<>();
    private Object operation;
    private int total;
    private boolean closed;

    public NetTrace(String key) {
        this.key = key;
        if (owners.putIfAbsent(key, this) != null) throw new IllegalStateException("Network trace already exists");
    }

    static NetTrace find(String key) {
        return key == null ? null : owners.get(key);
    }

    public synchronized void start() {
        if (closed) throw new IllegalStateException("Network trace ended");
        entries.clear();
        total = 0;
        operation = new Object();
    }

    public synchronized void stop() {
        if (operation == null) return;
        for (Entry entry : entries) if (!entry.complete) entry.elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - entry.start);
        operation = null;
    }

    public synchronized Map<String, Object> snapshot() {
        List<Map<String, Object>> records = new ArrayList<>();
        for (Entry entry : entries) records.add(entry.snapshot(operation != null));
        return Map.of("records", records, "total", total, "truncated", total > LIMIT);
    }

    synchronized EventListener listener(Call call) {
        if (operation == null || closed) return EventListener.NONE;
        if (++total > LIMIT) return EventListener.NONE;
        Object current = operation;
        Entry entry = new Entry(call);
        entries.add(entry);
        return new EventListener() {
            @Override
            public void responseHeadersEnd(Call call, Response response) {
                synchronized (NetTrace.this) {
                    if (operation == current) entry.code = response.code();
                }
            }

            @Override
            public void callEnd(Call call) {
                finish("");
            }

            @Override
            public void callFailed(Call call, IOException error) {
                finish(error.toString());
            }

            private void finish(String error) {
                synchronized (NetTrace.this) {
                    if (operation != current) return;
                    entry.error = error.length() > 4096 ? error.substring(0, 4096) : error;
                    entry.elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - entry.start);
                    entry.complete = true;
                }
            }
        };
    }

    @Override
    public synchronized void close() {
        stop();
        closed = true;
        owners.remove(key, this);
    }

    private static final class Entry {
        final long start = System.nanoTime();
        final String method;
        final String url;
        final boolean urlTruncated;
        int code;
        long elapsed;
        boolean complete;
        String error = "";

        Entry(Call call) {
            method = call.request().method();
            String value = call.request().url().newBuilder().username("").password("").build().toString();
            urlTruncated = value.length() > 4096;
            url = urlTruncated ? value.substring(0, 4096) : value;
        }

        Map<String, Object> snapshot(boolean running) {
            long duration = complete || !running ? elapsed : TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            return Map.of(
                    "method", method,
                    "url", url,
                    "code", code,
                    "elapsed", duration,
                    "complete", complete,
                    "error", error,
                    "urlTruncated", urlTruncated);
        }
    }
}
