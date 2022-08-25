package com.fongmi.android.tv;

import android.app.Application;

import com.fongmi.android.tv.ui.activity.CrashActivity;

import cat.ereza.customactivityoncrash.config.CaocConfig;

public class App extends Application {

    private static App instance;

    public App() {
        instance = this;
    }

    public static App get() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CaocConfig.Builder.create().backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT).errorActivity(CrashActivity.class).apply();
    }
}