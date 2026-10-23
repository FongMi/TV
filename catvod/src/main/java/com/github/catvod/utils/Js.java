package com.github.catvod.utils;

import com.github.catvod.runtime.QuickJsRuntime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class Js implements AutoCloseable {

    private final Set<CompletableFuture<String>> pending = new HashSet<>();
    private boolean closed;

    public String eval(String source) throws Exception {
        return eval(source, "{}");
    }

    public String eval(String source, String options) throws Exception {
        CompletableFuture<String> result = evalAsync(source, options);
        try {
            return result.get();
        } catch (InterruptedException error) {
            result.cancel(true);
            Thread.currentThread().interrupt();
            throw error;
        } catch (ExecutionException error) {
            if (error.getCause() instanceof Exception cause) throw cause;
            if (error.getCause() instanceof Error cause) throw cause;
            throw new IllegalStateException(error.getCause());
        }
    }

    public synchronized CompletableFuture<String> evalAsync(String source, String options) {
        if (closed) throw new IllegalStateException("Script owner closed");
        CompletableFuture<String> result = RuntimeModule.QUICKJS.entry(QuickJsRuntime.class).evaluate(source, options);
        pending.add(result);
        result.whenComplete((value, error) -> release(result));
        return result;
    }

    private synchronized void release(CompletableFuture<String> result) {
        pending.remove(result);
    }

    @Override
    public void close() {
        List<CompletableFuture<String>> active;
        synchronized (this) {
            if (closed) return;
            closed = true;
            active = new ArrayList<>(pending);
            pending.clear();
        }
        for (CompletableFuture<String> result : active) result.cancel(true);
    }
}
