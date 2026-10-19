package com.github.catvod.crawler;

import android.text.TextUtils;

import com.orhanobut.logger.Logger;

public class SpiderDebug {

    private static final String TAG = SpiderDebug.class.getSimpleName();
    private static final int MAX_LENGTH = 4096;

    public static void log(Throwable th) {
        if (th != null) th.printStackTrace();
    }

    public static void log(String msg) {
        if (!TextUtils.isEmpty(msg)) Logger.t(TAG).d(trim(msg));
    }

    public static void log(String tag, String msg, Object... args) {
        if (!TextUtils.isEmpty(msg)) Logger.t(tag).d(trim(args != null && args.length > 0 ? String.format(msg, args) : msg));
    }

    private static String trim(String message) {
        return message.length() <= MAX_LENGTH ? message : message.substring(0, MAX_LENGTH) + " [" + message.length() + " chars]";
    }
}
