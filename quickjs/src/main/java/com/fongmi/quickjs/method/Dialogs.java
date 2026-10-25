package com.fongmi.quickjs.method;

import com.fongmi.quickjs.QuickJS;
import com.fongmi.quickjs.host.Host;

import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

final class Dialogs {

    private final Global global;
    private final Host host;
    private final Set<Request> requests = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    Dialogs(Global global, Host host) {
        this.global = global;
        this.host = host;
    }

    CompletableFuture<Object> open(QuickJS.Json options, String callback) {
        Request request = new Request(global.current(), callback);
        requests.add(request);
        try {
            global.checkInvocation();
            if (closed) throw new IllegalStateException("Dialog owner is closed");
            if (options == null) throw new IllegalArgumentException("Dialog options are required");
            request.open(options.text());
        } catch (RuntimeException error) {
            request.result.completeExceptionally(error);
        }
        return request.result;
    }

    void close() {
        closed = true;
        for (Request request : requests) request.result.cancel(true);
    }

    private final class Request {

        private final Invocation invocation;
        private final String callback;
        private final CompletableFuture<Object> result = new CompletableFuture<>();
        private final Set<CompletableFuture<?>> actions = ConcurrentHashMap.newKeySet();
        private volatile CompletableFuture<String> source;

        private Request(Invocation invocation, String callback) {
            this.invocation = invocation;
            this.callback = callback;
            result.whenComplete((value, error) -> release());
        }

        private void open(String options) {
            if (invocation != null) invocation.await(result);
            if (result.isDone()) return;
            source = callback == null ? host.dialog(options) : host.dialog(options, this::onAction);
            source.whenComplete((value, error) -> {
                if (error == null) result.complete(value == null ? null : new QuickJS.Json(value));
                else result.completeExceptionally(error);
            });
            if (closed || result.isDone()) source.cancel(true);
            if (closed) result.cancel(true);
        }

        private CompletableFuture<String> onAction(String event) {
            if (result.isDone()) return CompletableFuture.failedFuture(new CancellationException("Dialog is closed"));
            Runnable finish = invocation == null ? () -> {} : invocation.beginDialogAction();
            CompletableFuture<Object> call = global.invokeDialogAction(invocation, callback, event, future -> {
                actions.add(future);
                if (result.isDone()) future.cancel(false);
            });
            actions.add(call);
            CompletableFuture<String> response = call.thenApply(value -> (String) value);
            response.whenComplete((value, error) -> {
                actions.remove(call);
                finish.run();
                if (response.isCancelled()) call.cancel(false);
            });
            if (result.isDone()) call.cancel(false);
            return response;
        }

        private void release() {
            requests.remove(this);
            if (source != null && !source.isDone()) source.cancel(true);
            for (CompletableFuture<?> action : actions) action.cancel(false);
            if (callback != null) global.releaseDialogAction(callback);
        }
    }
}
