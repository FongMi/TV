package com.github.catvod.utils;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class CallTimeout implements AutoCloseable {

    private static final ThreadLocal<CallTimeout> CURRENT = new ThreadLocal<>();

    private final Set<CompletableFuture<?>> pending = ConcurrentHashMap.newKeySet();

    private final CallTimeout parent = CURRENT.get();
    private final boolean enabled;
    private long remaining;
    private long resumed = System.nanoTime();
    private long generation;
    private int waits;
    private boolean closed;
    private Runnable expired;
    private ScheduledFuture<?> scheduled;

    public CallTimeout(long timeoutMillis) {
        if (timeoutMillis < 0) throw new IllegalArgumentException("Timeout must be nonnegative");
        enabled = timeoutMillis > 0;
        remaining = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
    }

    public static Runnable suspendCurrent() {
        CallTimeout current = CURRENT.get();
        return current == null ? () -> {} : current.suspend();
    }

    public static void trackCurrent(CompletableFuture<?> future) {
        CallTimeout current = CURRENT.get();
        if (current != null) current.track(future);
    }

    private void track(CompletableFuture<?> future) {
        pending.add(future);
        future.whenComplete((value, error) -> pending.remove(future));
        boolean ended;
        synchronized (this) {
            ended = closed;
        }
        if (ended) future.cancel(true);
    }

    public Runnable enter() {
        CallTimeout previous = CURRENT.get();
        CURRENT.set(this);
        return () -> {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        };
    }

    public synchronized void start(Runnable expired) {
        this.expired = expired;
        schedule();
    }

    public Runnable suspend() {
        Runnable resumeParent = parent == null ? () -> {} : parent.suspend();
        synchronized (this) {
            if (!closed && waits++ == 0) {
                remaining -= System.nanoTime() - resumed;
                cancel();
            }
        }
        return () -> {
            synchronized (this) {
                if (!closed && --waits == 0) {
                    resumed = System.nanoTime();
                    schedule();
                }
            }
            resumeParent.run();
        };
    }

    private void schedule() {
        if (closed || !enabled || expired == null || waits > 0) return;
        long current = ++generation;
        long delay = Math.max(0, remaining - (System.nanoTime() - resumed));
        scheduled = Task.scheduler().schedule(() -> expire(current), delay, TimeUnit.NANOSECONDS);
    }

    private void expire(long current) {
        synchronized (this) {
            if (closed || waits > 0 || current != generation) return;
            closed = true;
        }
        try {
            expired.run();
        } finally {
            cancelPending();
        }
    }

    private void cancel() {
        generation++;
        if (scheduled != null) scheduled.cancel(false);
    }

    @Override
    public void close() {
        synchronized (this) {
            closed = true;
            cancel();
        }
        cancelPending();
    }

    private void cancelPending() {
        for (CompletableFuture<?> future : pending) future.cancel(true);
    }
}
