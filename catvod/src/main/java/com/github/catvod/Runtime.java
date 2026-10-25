package com.github.catvod;

import android.app.Activity;
import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Runtime {

    private volatile Supplier<Activity> activity;
    private volatile Consumer<String> toast;
    private volatile BiFunction<String, Function<String, CompletableFuture<String>>, CompletableFuture<String>> dialog;
    private WeakReference<Context> context;

    private static Runtime get() {
        return Loader.INSTANCE;
    }

    public static void set(Context context) {
        get().context = new WeakReference<>(context);
    }

    public static Context context() {
        return get().context.get();
    }

    public static void setActivity(Supplier<Activity> activity) {
        get().activity = activity;
    }

    public static Activity activity() {
        Supplier<Activity> supplier = get().activity;
        Activity activity = supplier == null ? null : supplier.get();
        return activity == null || activity.isFinishing() || activity.isDestroyed() ? null : activity;
    }

    public static void setToast(Consumer<String> toast) {
        get().toast = toast;
    }

    public static void toast(String text) {
        get().toast.accept(text);
    }

    public static void setDialog(BiFunction<String, Function<String, CompletableFuture<String>>, CompletableFuture<String>> dialog) {
        get().dialog = dialog;
    }

    public static CompletableFuture<String> dialog(String options, Function<String, CompletableFuture<String>> onAction) {
        var dialog = get().dialog;
        if (dialog == null) return CompletableFuture.failedFuture(new UnsupportedOperationException("Dialog requires a compatible App"));
        return dialog.apply(options, onAction);
    }

    private static class Loader {
        static volatile Runtime INSTANCE = new Runtime();
    }
}
