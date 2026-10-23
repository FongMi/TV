package com.fongmi.quickjs.utils;

import com.fongmi.quickjs.QuickJS;
import com.fongmi.quickjs.QuickJSLoader;
import com.github.catvod.utils.Crypto;
import com.github.catvod.utils.Json;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;

public final class Scripts {

    private static final Gson GSON = new Gson();
    private static final String RUN = """
            ((globals, source) => {
                const target = globalThis, evaluate = eval, stringify = JSON.stringify;
                const finite = Number.isFinite, number = Number, define = Object.defineProperty;
                const tag = Function.prototype.call.bind(Object.prototype.toString);
                for (const key of Object.keys(globals)) define(target, key, {value: globals[key], writable: true, configurable: true});
                return stringify(evaluate(source), (key, value) => {
                    const type = typeof value;
                    if (value === undefined || type === 'function' || type === 'symbol'
                        || type === 'number' && !finite(value) || tag(value) === '[object Number]' && !finite(number(value))
                        || tag(value) === '[object Map]' || tag(value) === '[object Set]' || tag(value) === '[object Promise]') throw new TypeError('Script result must be JSON');
                    return value;
                });
            })
            """;

    private Scripts() {
    }

    public static CompletableFuture<String> evaluate(String source, String options) {
        if (source == null) throw new IllegalArgumentException("Script source is required");
        JsonObject settings = Json.strict(options == null ? "{}" : options).getAsJsonObject();
        long timeout = timeout(settings);
        String script = buildScript(source, settings);
        QuickJS context = new QuickJS(QuickJSLoader::init);
        try {
            context.bind("md5X", args -> Crypto.md5((String) args[0])).join();
            CompletableFuture<Object> evaluation = context.evaluateTimed(timeout, script, "spider-eval.js");
            return result(context, evaluation);
        } catch (RuntimeException | Error error) {
            try {
                context.close();
            } catch (RuntimeException | Error failure) {
                if (error != failure) error.addSuppressed(failure);
            }
            throw error;
        }
    }

    private static String buildScript(String source, JsonObject options) {
        JsonElement globals = options.has("globals") ? options.get("globals") : new JsonObject();
        if (!globals.isJsonObject() || globals.getAsJsonObject().has("md5X")) throw new IllegalArgumentException("Invalid script globals");
        return RUN + "(JSON.parse(" + GSON.toJson(globals.toString()) + ")," + GSON.toJson(source) + ")";
    }

    private static long timeout(JsonObject options) {
        if (!options.has("timeout")) return 5000;
        JsonElement value = options.get("timeout");
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Invalid script timeout");
        long timeout = value.getAsBigDecimal().longValueExact();
        if (timeout < 1 || timeout > 9007199254740991L) throw new IllegalArgumentException("Invalid script timeout");
        return timeout;
    }

    private static CompletableFuture<String> result(QuickJS context, CompletableFuture<Object> evaluation) {
        CompletableFuture<String> result = new CompletableFuture<>();
        result.whenComplete((value, error) -> {
            if (result.isCancelled()) evaluation.cancel(true);
        });
        evaluation.whenComplete((value, error) -> {
            try {
                context.close();
            } catch (RuntimeException | Error failure) {
                if (error == null) error = failure;
                else if (error != failure) error.addSuppressed(failure);
            }
            if (error == null) result.complete((String) value);
            else result.completeExceptionally(error);
        });
        return result;
    }
}
