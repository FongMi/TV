package com.whl.quickjs.android;

/**
 * Created by Harlon Wang on 2022/8/12.
 */
public final class QuickJSLoader {

    public static void init() {
        System.loadLibrary("quickjs-android-wrapper");
    }

    /**
     * Start threads to show stdout and stderr in logcat.
     *
     * @param tag Android Tag
     */
    public native static void startRedirectingStdoutStderr(String tag);
}
