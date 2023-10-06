package com.fongmi.quickjs.method;

import com.whl.quickjs.wrapper.JSCallFunction;
import com.whl.quickjs.wrapper.JSFunction;
import com.whl.quickjs.wrapper.JSObject;

import java9.util.concurrent.CompletableFuture;

public class Async {

    private final CompletableFuture<Object> future;

    public static CompletableFuture<Object> run(JSObject object, String name, Object[] args) {
        return new Async().call(object, name, args);
    }

    private Async() {
        this.future = new CompletableFuture<>();
    }

    private CompletableFuture<Object> call(JSObject object, String name, Object[] args) {
        JSFunction function = object.getJSFunction(name);
        if (function == null) return empty();
        Object result = function.call(args);
        if (result instanceof JSObject) return then(result);
        future.complete(result);
        return future;
    }

    private CompletableFuture<Object> empty() {
        future.complete(null);
        return future;
    }

    private CompletableFuture<Object> then(Object result) {
        JSObject promise = (JSObject) result;
        JSFunction then = promise.getJSFunction("then");
        if (then != null) then.call(callback);
        return future;
    }

    private final JSCallFunction callback = new JSCallFunction() {
        @Override
        public Object call(Object... args) {
            future.complete(args[0]);
            return null;
        }
    };
}
