package com.hiker.drpy;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Worker {

    private final ExecutorService executor;

    private static class Loader {
        static volatile Worker INSTANCE = new Worker();
    }

    private static Worker get() {
        return Loader.INSTANCE;
    }

    public Worker() {
        executor = Executors.newSingleThreadExecutor();
    }

    public static void submit(Runnable runnable) {
        get().executor.submit(runnable);
    }

    public static Future<?> submit(Callable<String> callable) {
        return get().executor.submit(callable);
    }
}
