package com.github.catvod.crawler;

import com.orhanobut.logger.Logger;

public class SpiderDebug {

    private static final String TAG = SpiderDebug.class.getSimpleName();

    public static void log(Throwable th) {
        Logger.t(TAG).d(th.getMessage());
    }

    public static void log(String msg) {
        Logger.t(TAG).d(msg);
    }

    public static void xml(String msg) {
        Logger.t(TAG).xml(msg);
    }
}
