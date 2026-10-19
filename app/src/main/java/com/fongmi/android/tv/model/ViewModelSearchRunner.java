package com.fongmi.android.tv.model;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.github.catvod.utils.Task;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

final class ViewModelSearchRunner {

    private final List<Future<?>> futures;
    private final AtomicInteger epoch;

    ViewModelSearchRunner() {
        futures = new CopyOnWriteArrayList<>();
        epoch = new AtomicInteger(0);
    }

    void start(List<Site> sites, Function<Site, Callable<Result>> taskFactory, Consumer<Result> onResult) {
        int current = nextEpoch();
        cancelFutures();
        sites.forEach(site -> execute(taskFactory.apply(site), current, onResult));
    }

    void stop() {
        nextEpoch();
        cancelFutures();
    }

    private int nextEpoch() {
        return epoch.incrementAndGet();
    }

    private void cancelFutures() {
        futures.forEach(future -> future.cancel(true));
        futures.clear();
    }

    private void execute(Callable<Result> callable, int current, Consumer<Result> onResult) {
        FluentFuture<Result> future = FluentFuture.from(Task.largeExecutor().submit(callable)).withTimeout(Constant.TIMEOUT_SEARCH, TimeUnit.MILLISECONDS, Task.scheduler());
        futures.add(future);
        future.addCallback(Task.callback(
                result -> {
                    if (epoch.get() == current) onResult.accept(result);
                }
        ), MoreExecutors.directExecutor());
    }
}
