package com.fongmi.android.tv;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;

import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.ui.activity.CrashActivity;
import com.fongmi.android.tv.utils.Notify;
import com.github.catvod.Init;
import com.github.catvod.bean.Doh;
import com.github.catvod.net.OkHttp;
import com.google.gson.Gson;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.LogAdapter;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cat.ereza.customactivityoncrash.config.CaocConfig;

public class App extends Application {

    private final ExecutorService executor;
    private final Handler handler;
    private static App instance;
    private Activity activity;
    private final Gson gson;
    private boolean hook;

    public App() {
        instance = this;
        executor = Executors.newFixedThreadPool(Constant.THREAD_POOL);
        handler = HandlerCompat.createAsync(Looper.getMainLooper());
        gson = new Gson();
    }

    public static App get() {
        return instance;
    }

    public static Gson gson() {
        return get().gson;
    }

    public static Activity activity() {
        return get().activity;
    }

    public static void execute(Runnable runnable) {
        get().executor.execute(runnable);
    }

    public static void post(Runnable runnable) {
        get().handler.post(runnable);
    }

    public static void post(Runnable runnable, long delayMillis) {
        get().handler.removeCallbacks(runnable);
        if (delayMillis >= 0) get().handler.postDelayed(runnable, delayMillis);
    }

    public static void removeCallbacks(Runnable runnable) {
        get().handler.removeCallbacks(runnable);
    }

    public static void removeCallbacks(Runnable... runnable) {
        for (Runnable r : runnable) get().handler.removeCallbacks(r);
    }

    public void setHook(boolean hook) {
        this.hook = hook;
    }

    private void setActivity(Activity activity) {
        this.activity = activity;
    }

    private LogAdapter getLogAdapter() {
        return new AndroidLogAdapter(PrettyFormatStrategy.newBuilder().showThreadInfo(false).tag("").build()) {
            @Override
            public boolean isLoggable(int priority, String tag) {
                return true;
            }
        };
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Init.set(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Notify.createChannel();
        Logger.addLogAdapter(getLogAdapter());
        OkHttp.get().setDoh(Doh.objectFrom(Setting.getDoh()));
        CaocConfig.Builder.create().backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT).errorActivity(CrashActivity.class).apply();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if (activity != activity()) setActivity(activity);
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (activity != activity()) setActivity(activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                if (activity != activity()) setActivity(activity);
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                if (activity == activity()) setActivity(null);
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                if (activity == activity()) setActivity(null);
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (activity == activity()) setActivity(null);
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }
        });
    }

    @Override
    public PackageManager getPackageManager() {
        if (!hook) return getBaseContext().getPackageManager();
        return LiveConfig.get().getHome().getCore();
    }

    @Override
    public String getPackageName() {
        if (!hook) return getBaseContext().getPackageName();
        return LiveConfig.get().getHome().getCore().getPkg();
    }
}