package com.github.catvod.crawler;

import android.text.TextUtils;

import com.orhanobut.logger.Logger;

public class SpiderDebug {

    private static final String TAG = SpiderDebug.class.getSimpleName();

    public static void log(Throwable th) {
        if (th == null || TextUtils.isEmpty(th.getMessage())) return;
        Logger.t(TAG).d(th.getMessage());
    }

    public static void log(String msg) {
        if (TextUtils.isEmpty(msg)) return;
        Logger.t(TAG).d(msg);
    }
}
