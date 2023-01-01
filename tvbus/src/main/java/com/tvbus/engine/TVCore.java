package com.tvbus.engine;

import android.content.Context;
import android.text.TextUtils;

public class TVCore {

    private final long handle;

    public TVCore() {
        PmsHook.inject();
        System.loadLibrary("tvcore");
        handle = initialise();
    }

    public TVCore listener(Listener listener) {
        setListener(handle, listener);
        return this;
    }

    public TVCore play(int port) {
        setPlayPort(handle, port);
        return this;
    }

    public TVCore serv(int port) {
        setServPort(handle, port);
        return this;
    }

    public TVCore mode(int mode) {
        setRunningMode(handle, mode);
        return this;
    }

    public TVCore auth(String str) {
        if (!TextUtils.isEmpty(str)) setAuthUrl(handle, str);
        return this;
    }

    public TVCore broker(String str) {
        if (!TextUtils.isEmpty(str)) setMKBroker(handle, str);
        return this;
    }

    public TVCore name(String str) {
        if (!TextUtils.isEmpty(str)) setUsername(handle, str);
        return this;
    }

    public TVCore pass(String str) {
        if (!TextUtils.isEmpty(str)) setPassword(handle, str);
        return this;
    }

    public void init(Context context) {
        new Thread(() -> {
            init(handle, context);
            run(handle);
        }).start();
    }

    public void start(String url) {
        start(handle, url);
    }

    public void stop() {
        stop(handle);
    }

    public void quit() {
        quit(handle);
    }

    private native long initialise();

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

    private native void setListener(long handle, Listener listener);
}