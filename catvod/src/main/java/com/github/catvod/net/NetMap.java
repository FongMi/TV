package com.github.catvod.net;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

final class NetMap<T, R> {

    private static final ExecutorService WORKERS = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "Spider map");
        thread.setDaemon(true);
        return thread;
    });

    private final List<T> items;
    private final List<R> values;
    private final Net.Mapper<T, R> mapper;
    private final List<FutureTask<Void>> workers = new ArrayList<>();
    private final CompletableFuture<List<R>> result = new CompletableFuture<>();
    private final AtomicInteger remaining;
    private int index;
    private boolean stopped;

    NetMap(List<T> items, Net.Mapper<T, R> mapper, int limit, Consumer<NetMap<?, ?>> complete) {
        this.items = new ArrayList<>(items);
        this.values = new ArrayList<>(Collections.nCopies(this.items.size(), null));
        this.mapper = mapper;
        int count = Math.min(this.items.size(), limit);
        remaining = new AtomicInteger(count);
        for (int i = 0; i < count; i++) workers.add(createWorker(complete));
    }

    private FutureTask<Void> createWorker(Consumer<NetMap<?, ?>> complete) {
        return new FutureTask<>(this::work, null) {
            @Override
            protected void done() {
                if (remaining.decrementAndGet() != 0) return;
                result.complete(values);
                complete.accept(NetMap.this);
            }
        };
    }

    CompletableFuture<List<R>> start() {
        for (FutureTask<Void> worker : workers) WORKERS.execute(worker);
        return result;
    }

    private synchronized int next() {
        return stopped || index == items.size() ? -1 : index++;
    }

    private void work() {
        try {
            for (int current; (current = next()) >= 0;) values.set(current, mapper.apply(items.get(current), current));
        } catch (Throwable error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            fail(error);
        }
    }

    private synchronized void fail(Throwable error) {
        stopped = true;
        result.completeExceptionally(error);
    }

    void cancel() {
        fail(new InterruptedIOException("Map cancelled"));
        for (FutureTask<Void> worker : workers) worker.cancel(true);
    }
}
