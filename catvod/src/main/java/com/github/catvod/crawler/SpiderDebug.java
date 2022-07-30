package com.github.catvod.crawler;

import android.util.Log;

public class SpiderDebug {

    private static final String TAG = SpiderDebug.class.getSimpleName();

    public static void log(Throwable th) {
        Log.d(TAG, th.getMessage());
    }

    public static void log(String msg) {
        Log.d(TAG, msg);
    }
}
