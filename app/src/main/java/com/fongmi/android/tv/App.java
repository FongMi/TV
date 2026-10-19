package com.fongmi.android.tv;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.HandlerCompat;
import androidx.media3.mpvplayer.MpvLibrary;

import com.fongmi.android.tv.player.mpv.MpvUtil;
import com.fongmi.android.tv.receiver.RuntimeReceiver;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.RuntimeInstaller;
import com.fongmi.hook.Hook;
import com.github.catvod.Runtime;
import com.github.catvod.utils.RuntimeModule;
import com.google.gson.Gson;

import java.io.IOException;

public class App extends Application implements Application.ActivityLifecycleCallbacks {

    private static volatile App instance;

    private final Handler handler;
    private final Gson gson;
    private final long time;

    private volatile Activity activity;
    private Hook hook;

    public App() {
        instance = this;
        gson = new Gson();
        time = System.currentTimeMillis();
        handler = HandlerCompat.createAsync(Looper.getMainLooper());
    }

    public static App get() {
        return instance;
    }

    public static Gson gson() {
        return get().gson;
    }

    public static long time() {
        return get().time;
    }

    public static Activity activity() {
        return get().activity;
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

    public void setHook(Hook hook) {
        this.hook = hook;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Runtime.set(base);
        Runtime.setActivity(App::activity);
        Runtime.setToast(text -> App.post(() -> Notify.show(text)));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initRuntime();
        Notify.createChannel();
        registerActivityLifecycleCallbacks(this);
    }

    private void initRuntime() {
        MpvLibrary.setLibraryLoader(MpvUtil::loadRuntime);
        RuntimeModule.refresh();
        cleanRuntime();
        RuntimeReceiver.register(this);
    }

    private void cleanRuntime() {
        if (!isMainProcess()) return;
        for (RuntimeModule module : RuntimeModule.values()) if (module != RuntimeModule.NODEJS) cleanRuntime(module);
    }

    private void cleanRuntime(RuntimeModule module) {
        try {
            module.clean();
        } catch (IOException e) {
            Log.w("RuntimeModule", "Unable to clean runtime cache", e);
        }
    }

    private boolean isMainProcess() {
        String name = getBaseContext().getPackageName();
        if (Build.VERSION.SDK_INT >= 28) return name.equals(Application.getProcessName());
        var processes = getSystemService(ActivityManager.class).getRunningAppProcesses();
        if (processes != null) for (var process : processes) if (process.pid == Process.myPid()) return name.equals(process.processName);
        return false;
    }

    @Override
    public PackageManager getPackageManager() {
        return hook != null ? hook : getBaseContext().getPackageManager();
    }

    @Override
    public String getPackageName() {
        return hook != null ? hook.getPackageName() : getBaseContext().getPackageName();
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (activity != activity()) this.activity = activity;
        RuntimeModule.refresh();
        RuntimeInstaller.resumeUpdate(activity);
        Updater.get().notifyModules();
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (activity == activity()) this.activity = null;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }
}
