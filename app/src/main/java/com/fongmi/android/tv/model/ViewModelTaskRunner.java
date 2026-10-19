package com.fongmi.android.tv.model;

import com.github.catvod.utils.Task;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

final class ViewModelTaskRunner<T extends Enum<T>> {

    private final Map<T, ListenableFuture<?>> futures;
    private final Map<T, AtomicInteger> taskIds;

    ViewModelTaskRunner(Class<T> typeClass) {
        futures = new EnumMap<>(typeClass);
        taskIds = new EnumMap<>(typeClass);
        for (T type : typeClass.getEnumConstants()) taskIds.put(type, new AtomicInteger(0));
    }

    <R> void execute(T type, long timeoutMs, Callable<R> callable, Consumer<R> onSuccess, Consumer<Throwable> onError) {
        AtomicInteger taskId = Objects.requireNonNull(taskIds.get(type));
        int currentId = taskId.incrementAndGet();
        ListenableFuture<?> old = futures.get(type);
        if (old != null) old.cancel(true);
        FluentFuture<R> future = FluentFuture.from(Task.executor().submit(callable)).withTimeout(timeoutMs, TimeUnit.MILLISECONDS, Task.scheduler());
        futures.put(type, future);
        future.addCallback(Task.callback(
                result -> {
                    if (taskId.get() == currentId) onSuccess.accept(result);
                },
                error -> {
                    if (error instanceof CancellationException) return;
                    if (taskId.get() == currentId) onError.accept(error);
                }
        ), MoreExecutors.directExecutor());
    }

    void cancel(T type) {
        Objects.requireNonNull(taskIds.get(type)).incrementAndGet();
        ListenableFuture<?> future = futures.remove(type);
        if (future != null) future.cancel(true);
    }

    void cancelAll() {
        futures.values().forEach(future -> future.cancel(true));
        futures.clear();
    }
}
