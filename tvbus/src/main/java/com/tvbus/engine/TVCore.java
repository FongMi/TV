package com.tvbus.engine;

import android.content.Context;
import android.text.TextUtils;

public class TVCore {

    private long nativeHandle;

    private static class Loader {
        static volatile TVCore INSTANCE = new TVCore();
    }

    public static TVCore getInstance() {
        return Loader.INSTANCE;
    }

    private TVCore() {
        try {
            PmsHook.inject();
            System.loadLibrary("tvcore");
            nativeHandle = initialise();
        } catch (Throwable ignored) {
        }
    }

    public TVCore listener(TVListener listener) {
        try {
            setListener(nativeHandle, listener);
        } catch (Throwable ignored) {
        }
        return this;
    }

    public TVCore play(int port) {
        try {
            setPlayPort(nativeHandle, port);
        } catch (Throwable ignored) {
        }
        return this;
    }

    public TVCore serv(int port) {
        try {
            setServPort(nativeHandle, port);
        } catch (Throwable ignored) {
        }
        return this;
    }

    public TVCore mode(int mode) {
        try {
            setRunningMode(nativeHandle, mode);
        } catch (Throwable ignored) {
        }
        return this;
    }

    public TVCore auth(String str) {
        try {
            if (!TextUtils.isEmpty(str)) setAuthUrl(nativeHandle, str);
        } catch (Throwable ignored) {
        }
        return this;
    }

    public TVCore broker(String str) {
        try {
            if (!TextUtils.isEmpty(str)) setMKBroker(nativeHandle, str);
        } catch (Throwable ignored) {
        }
        return this;
    }

    public TVCore name(String str) {
        try {
            if (!TextUtils.isEmpty(str)) setUsername(nativeHandle, str);
        } catch (Throwable ignored) {
        }
        return this;
    }

    public TVCore pass(String str) {
        try {
            if (!TextUtils.isEmpty(str)) setPassword(nativeHandle, str);
        } catch (Throwable ignored) {
        }
        return this;
    }

    public void start(String url) {
        try {
            start(nativeHandle, url);
        } catch (Throwable ignored) {
        }
    }

    public void stop() {
        try {
            stop(nativeHandle);
        } catch (Throwable ignored) {
        }
    }

    void init(Context context) {
        try {
            init(nativeHandle, context);
            run(nativeHandle);
        } catch (Throwable ignored) {
        }
    }

    void quit() {
        try {
            quit(nativeHandle);
        } catch (Throwable ignored) {
        }
    }

    private native int init(long handle, Context context);

    private native int run(long handle);

    private native void start(long handle, String url);

    private native void stop(long handle);

    private native void quit(long handle);

    private native void setServPort(long handle, int iPort);

    private native void setPlayPort(long handle, int iPort);

    private native void setRunningMode(long handle, int mode);

    private native void setAuthUrl(long handle, String str);

    private native void setMKBroker(long handle, String str);

    private native void setPassword(long handle, String str);

    private native void setUsername(long handle, String str);

    private native void setListener(long handle, TVListener listener);

    private native long initialise();
}