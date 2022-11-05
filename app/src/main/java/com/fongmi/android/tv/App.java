package com.fongmi.android.tv;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.core.os.HandlerCompat;

import com.fongmi.android.tv.ui.activity.CrashActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cat.ereza.customactivityoncrash.config.CaocConfig;

public class App extends Application {

    private final ExecutorService executor;
    private final Handler handler;
    private static App instance;

    public App() {
        instance = this;
        executor = Executors.newFixedThreadPool(4);
        handler = HandlerCompat.createAsync(Looper.getMainLooper());
    }

    public static App get() {
        return instance;
    }

    public static void execute(Runnable runnable) {
        get().executor.execute(runnable);
    }

    public static void post(Runnable runnable) {
        get().handler.post(runnable);
    }

    public static void post(Runnable runnable, long delayMillis) {
        get().handler.removeCallbacks(runnable);
        get().handler.postDelayed(runnable, delayMillis);
    }

    public static void removeCallbacks(Runnable runnable) {
        get().handler.removeCallbacks(runnable);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CaocConfig.Builder.create().backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT).errorActivity(CrashActivity.class).apply();
    }
}