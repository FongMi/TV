package com.tvbus.engine;

import android.content.Context;

public class TVCore {

    private long handle;

    public TVCore() {
        try {
            PmsHook.inject();
            System.loadLibrary("tvcore");
            handle = initialise();
        } catch (Throwable ignored) {
        }
    }

    public void listener(Listener listener) {
        try {
            setListener(handle, listener);
        } catch (Throwable ignored) {
        }
    }

    public void play(int port) {
        try {
            setPlayPort(handle, port);
        } catch (Throwable ignored) {
        }
    }

    public void serv(int port) {
        try {
            setServPort(handle, port);
        } catch (Throwable ignored) {
        }
    }

    public void mode(int mode) {
        try {
            setRunningMode(handle, mode);
        } catch (Throwable ignored) {
        }
    }

    public void auth(String str) {
        try {
            setAuthUrl(handle, str);
        } catch (Throwable ignored) {
        }
    }

    public void broker(String str) {
        try {
            setMKBroker(handle, str);
        } catch (Throwable ignored) {
        }
    }

    public void name(String str) {
        try {
            setUsername(handle, str);
        } catch (Throwable ignored) {
        }
    }

    public void pass(String str) {
        try {
            setPassword(handle, str);
        } catch (Throwable ignored) {
        }
    }

    public void init(Context context) {
        new Thread(() -> start(context)).start();
    }

    private void start(Context context) {
        try {
            init(handle, context);
            run(handle);
        } catch (Throwable ignored) {
        }
    }

    public void start(String url) {
        try {
            start(handle, url);
        } catch (Throwable ignored) {
        }
    }

    public void stop() {
        try {
            stop(handle);
        } catch (Throwable ignored) {
        }
    }

    public void quit() {
        try {
            quit(handle);
        } catch (Throwable ignored) {
        }
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