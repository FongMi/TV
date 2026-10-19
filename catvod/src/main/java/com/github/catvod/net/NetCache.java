package com.github.catvod.net;

import com.github.catvod.utils.Json;
import com.google.gson.JsonObject;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

final class NetCache implements AutoCloseable {

    private static final int CAPACITY = 64;
    private static final long MAX_BYTES = 16 * 1024 * 1024;
    private final LongSupplier clock;
    private final LongSupplier wallClock;
    private final Function<String, String> read;
    private final BiConsumer<String, String> write;
    private final Consumer<String> remove;
    private final Map<String, Entry> entries = new LinkedHashMap<>(16, 0.75f, true);
    private final Set<Flight> pending = new HashSet<>();
    private boolean closed;

    NetCache() {
        this(System::nanoTime);
    }

    NetCache(LongSupplier clock) {
        this(clock, System::currentTimeMillis, null, null, null);
    }

    NetCache(NetStore store) {
        this(System::nanoTime, System::currentTimeMillis, store::get, store::set, store::remove);
    }

    NetCache(LongSupplier clock, LongSupplier wallClock, Function<String, String> read, BiConsumer<String, String> write, Consumer<String> remove) {
        this.clock = clock;
        this.wallClock = wallClock;
        this.read = read;
        this.write = write;
        this.remove = remove;
    }

    synchronized String peek(String key) {
        if (closed) throw new IllegalStateException("Network owner closed");
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("Invalid cache key");
        Entry entry = entries.get(key);
        if (entry == null && read != null) entry = restore(key);
        return entry == null ? null : entry.value;
    }

    synchronized String value(String key, CacheOptions options) {
        if (closed) throw new IllegalStateException("Network owner closed");
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("Invalid cache key");
        if (options.persist && read == null) throw new IllegalArgumentException("Persistent cache requires a site owner");
        Entry entry = options.ttl == 0 ? null : entries.get(key);
        if (options.ttl > 0 && entry == null && options.persist) entry = restore(key);
        return fresh(entry, options.ttl) ? entry.value : null;
    }

    CompletableFuture<String> cached(String key, CacheOptions options, Supplier<CompletableFuture<String>> loader) {
        Flight flight;
        String stale;
        synchronized (this) {
            if (closed) return Net.failedFuture(canceled());
            if (key == null || key.isEmpty() || loader == null) return Net.failedFuture(new IllegalArgumentException("Invalid cache options"));
            if (options.persist && read == null) return Net.failedFuture(new IllegalArgumentException("Persistent cache requires a site owner"));
            Entry entry = options.ttl == 0 ? null : entries.get(key);
            if (options.ttl > 0 && entry == null && options.persist) entry = restore(key);
            if (fresh(entry, options.ttl)) return CompletableFuture.completedFuture(entry.value);
            if (entry != null && entry.flight != null) return entry.value == null || !options.stale ? copy(entry.flight.result) : CompletableFuture.completedFuture(entry.value);
            if (options.ttl > 0 && entry == null) {
                if (entries.size() == CAPACITY && !evict()) return Net.failedFuture(new IllegalStateException("Cache capacity exceeded"));
                entry = new Entry();
                entries.put(key, entry);
            }
            flight = new Flight(key, entry, options.persist && options.ttl > 0);
            if (entry != null) entry.flight = flight;
            pending.add(flight);
            stale = entry == null || !options.stale ? null : entry.value;
        }
        start(flight, loader);
        return stale == null ? copy(flight.result) : CompletableFuture.completedFuture(stale);
    }

    private boolean fresh(Entry entry, long ttl) {
        return entry != null && entry.value != null && entry.age < ttl && TimeUnit.NANOSECONDS.toMillis(clock.getAsLong() - entry.loadedAt) < ttl - entry.age;
    }

    private Entry restore(String key) {
        String saved = read.apply(key);
        if (saved == null) return null;
        JsonObject record = Json.strict(saved).getAsJsonObject();
        Entry entry = new Entry();
        entry.value = record.get("value").toString();
        long savedAt = record.get("time").getAsLong();
        long now = wallClock.getAsLong();
        entry.age = savedAt > now ? Long.MAX_VALUE : Math.max(0, now - savedAt);
        entry.loadedAt = clock.getAsLong();
        if (entries.size() == CAPACITY && !evict()) throw new IllegalStateException("Cache capacity exceeded");
        entries.put(key, entry);
        trim();
        return entry;
    }

    private boolean evict() {
        Iterator<Entry> values = entries.values().iterator();
        while (values.hasNext()) {
            if (values.next().flight != null) continue;
            values.remove();
            return true;
        }
        return false;
    }

    private static CompletableFuture<String> copy(CompletableFuture<String> result) {
        return result.thenApply(value -> value);
    }

    private void start(Flight flight, Supplier<CompletableFuture<String>> loader) {
        if (flight.result.isDone()) return;
        CompletableFuture<String> loading;
        try {
            loading = loader.get();
            if (loading == null) throw new IllegalArgumentException("Cache loader must return a future");
        } catch (Throwable error) {
            finish(flight, null, error);
            return;
        }
        boolean canceled;
        synchronized (this) {
            canceled = !pending.contains(flight);
            if (!canceled) flight.loading = loading;
        }
        if (canceled) {
            loading.cancel(true);
            return;
        }
        loading.whenComplete((value, error) -> finish(flight, value, error == null ? validate(value) : error));
    }

    private Throwable validate(String value) {
        try {
            if (value == null || value.isBlank()) throw new IllegalArgumentException("Cache loader must return JSON");
            Json.strict(value);
            return null;
        } catch (RuntimeException invalid) {
            return new IllegalArgumentException("Cache loader must return valid JSON");
        }
    }

    private void finish(Flight flight, String value, Throwable error) {
        synchronized (this) {
            if (!pending.remove(flight)) return;
            try {
                update(flight, value, error);
            } catch (RuntimeException failure) {
                error = failure;
            }
        }
        if (error == null) flight.result.complete(value);
        else flight.result.completeExceptionally(error);
    }

    private void update(Flight flight, String value, Throwable error) {
        if (flight.entry == null) return;
        flight.entry.flight = null;
        if (error != null) {
            if (flight.entry.value == null) entries.remove(flight.key);
            return;
        }
        if (flight.persist) write.accept(flight.key, "{\"time\":" + wallClock.getAsLong() + ",\"value\":" + value + "}");
        flight.entry.value = value;
        flight.entry.loadedAt = clock.getAsLong();
        flight.entry.age = 0;
        trim();
    }

    private void trim() {
        long bytes = 0;
        for (Entry entry : entries.values()) if (entry.value != null) bytes += (long) entry.value.length() * 2;
        Iterator<Entry> values = entries.values().iterator();
        while (bytes > MAX_BYTES && values.hasNext()) {
            Entry entry = values.next();
            if (entry.flight != null) continue;
            if (entry.value != null) bytes -= (long) entry.value.length() * 2;
            values.remove();
        }
    }

    void clear(String key) {
        List<Flight> active = new ArrayList<>();
        synchronized (this) {
            if (closed) throw new IllegalStateException("Network owner closed");
            if (key != null && key.isEmpty()) throw new IllegalArgumentException("Invalid cache key");
            if (remove != null) remove.accept(key);
            pending.removeIf(flight -> {
                if (key != null && !key.equals(flight.key)) return false;
                active.add(flight);
                return true;
            });
            if (key == null) entries.clear();
            else entries.remove(key);
        }
        for (Flight flight : active) cancel(flight);
    }

    @Override
    public void close() {
        List<Flight> active;
        synchronized (this) {
            if (closed) return;
            closed = true;
            active = new ArrayList<>(pending);
            pending.clear();
            entries.clear();
        }
        for (Flight flight : active) cancel(flight);
    }

    private void cancel(Flight flight) {
        flight.result.completeExceptionally(canceled());
        if (flight.loading != null) flight.loading.cancel(true);
    }

    private static InterruptedIOException canceled() {
        return new InterruptedIOException("Network owner closed");
    }

    private static final class Entry {
        String value;
        long loadedAt;
        long age;
        Flight flight;
    }

    private static final class Flight {
        final String key;
        final Entry entry;
        final boolean persist;
        final CompletableFuture<String> result = new CompletableFuture<>();
        CompletableFuture<String> loading;

        Flight(String key, Entry entry, boolean persist) {
            this.key = key;
            this.entry = entry;
            this.persist = persist;
        }
    }
}
