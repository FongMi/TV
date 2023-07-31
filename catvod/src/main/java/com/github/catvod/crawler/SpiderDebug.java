package com.github.catvod.crawler;

import android.text.TextUtils;

import com.orhanobut.logger.Logger;

public class SpiderDebug {

    private static final String TAG = SpiderDebug.class.getSimpleName();

    public static void log(Throwable th) {
        if (th != null) th.printStackTrace();
    }

    public static void log(String msg) {
        if (!TextUtils.isEmpty(msg)) Logger.t(TAG).d(msg);
    }
}
