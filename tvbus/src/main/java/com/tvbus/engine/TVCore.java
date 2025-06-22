package com.tvbus.engine;

import android.content.Context;

import com.github.catvod.Init;

public class TVCore implements Runnable {

    private final Thread thread;
    private final long handle;

    public TVCore(String path) {
        System.load(path);
        handle = initialise();
        thread = new Thread(this);
    }

    public TVCore listener(Listener listener) {
        try {
            setListener(handle, listener);
            return this;
        } catch (Throwable ignored) {
            return this;
        }
    }

    public TVCore play(int port) {
        try {
            setPlayPort(handle, port);
            return this;
        } catch (Throwable ignored) {
            return this;
        }
    }

    public TVCore serv(int port) {
        try {
            setServPort(handle, port);
            return this;
        } catch (Throwable ignored) {
            return this;
        }
    }

    public TVCore mode(int mode) {
        try {
            setRunningMode(handle, mode);
            return this;
        } catch (Throwable ignored) {
            return this;
        }
    }

    public TVCore auth(String str) {
        try {
            if (!str.isEmpty()) setAuthUrl(handle, str);
            return this;
        } catch (Throwable ignored) {
            return this;
        }
    }

    public TVCore domain(String str) {
        try {
            if (!str.isEmpty()) setDomainSuffix(handle, str);
            return this;
        } catch (Throwable ignored) {
            return this;
        }
    }

    public TVCore broker(String str) {
        try {
            if (!str.isEmpty()) setMKBroker(handle, str);
            return this;
        } catch (Throwable ignored) {
            return this;
        }
    }

    public TVCore name(String str) {
        try {
            if (!str.isEmpty()) setUsername(handle, str);
            return this;
        } catch (Throwable ignored) {
            return this;
        }
    }

    public TVCore pass(String str) {
        try {
            if (!str.isEmpty()) setPassword(handle, str);
            return this;
        } catch (Throwable ignored) {
            return this;
        }
    }

    public TVCore init() {
        thread.start();
        return this;
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
            thread.interrupt();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void run() {
        try {
            init(handle, Init.context());
            run(handle);
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

    private native void setDomainSuffix(long handle, String str);

    private native void setMKBroker(long handle, String str);

    private native void setPassword(long handle, String str);

    private native void setUsername(long handle, String str);

    private native void setListener(long handle, Listener listener);
}