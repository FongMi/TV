package com.github.catvod.utils;

import android.os.Looper;

import com.github.catvod.Runtime;

import org.json.JSONObject;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public final class Dialog implements AutoCloseable {

    private final Set<CompletableFuture<String>> requests = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public JSONObject show(JSONObject options) throws Exception {
        return show(options, null);
    }

    public JSONObject show(JSONObject options, Callback onAction) throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IllegalStateException("Dialog.show requires a worker thread");
        if (options == null) throw new IllegalArgumentException("Dialog options are required");
        if (closed) throw new IllegalStateException("Dialog owner is closed");
        BlockingQueue<Runnable> actions = new LinkedBlockingQueue<>();
        CompletableFuture<String> source = Runtime.dialog(options.toString(), onAction == null ? null : event -> enqueue(actions, onAction, event));
        requests.add(source);
        source.whenComplete((value, error) -> {
            requests.remove(source);
            actions.add(() -> {});
        });
        CallTimeout.trackCurrent(source);
        if (closed) source.cancel(true);
        try {
            while (!source.isDone()) {
                Runnable action = waitForAction(actions);
                if (!source.isDone()) action.run();
            }
            String value = source.get();
            return value == null ? null : new JSONObject(value);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw error;
        } finally {
            if (!source.isDone()) source.cancel(true);
        }
    }

    private Runnable waitForAction(BlockingQueue<Runnable> actions) throws InterruptedException {
        Runnable resume = CallTimeout.suspendCurrent();
        try {
            return actions.take();
        } finally {
            resume.run();
        }
    }

    private CompletableFuture<String> enqueue(BlockingQueue<Runnable> actions, Callback callback, String event) {
        CompletableFuture<String> result = new CompletableFuture<>();
        actions.add(() -> invoke(callback, event, result));
        return result;
    }

    private void invoke(Callback callback, String event, CompletableFuture<String> result) {
        if (result.isDone()) return;
        try {
            JSONObject update = callback.onAction(new JSONObject(event));
            result.complete(update == null ? null : update.toString());
        } catch (Exception | Error error) {
            result.completeExceptionally(error);
        }
    }

    @Override
    public void close() {
        closed = true;
        for (CompletableFuture<String> request : requests) request.cancel(true);
    }

    public interface Callback {

        JSONObject onAction(JSONObject event) throws Exception;

    }
}
