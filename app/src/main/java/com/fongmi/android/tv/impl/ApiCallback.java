package com.fongmi.android.tv.impl;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public record ApiCallback<T>(int requestId, AtomicInteger currentId, Parser<T> parser, Consumer<T> success, Consumer<Exception> error) implements Callback {

    public static void post(int requestId, AtomicInteger currentId, Runnable action) {
        App.post(() -> {
            if (currentId.get() == requestId) action.run();
        });
    }

    @Override
    public void onResponse(@NonNull Call call, @NonNull Response response) {
        try (response) {
            T result = parser.parse(response);
            post(requestId, currentId, () -> success.accept(result));
        } catch (Exception e) {
            post(requestId, currentId, () -> error.accept(e));
        }
    }

    @Override
    public void onFailure(@NonNull Call call, @NonNull IOException e) {
        post(requestId, currentId, () -> error.accept(e));
    }

    public interface Parser<T> {

        T parse(Response response) throws Exception;
    }
}
