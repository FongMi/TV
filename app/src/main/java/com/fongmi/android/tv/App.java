package com.fongmi.android.tv;

import android.app.Application;

public class App extends Application {

    private static App instance;

    public App() {
        instance = this;
    }

    public static App get() {
        return instance;
    }
}