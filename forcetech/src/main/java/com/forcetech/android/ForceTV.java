package com.forcetech.android;

public class ForceTV {

    public void start(String lib, int port) {
        try {
            System.loadLibrary(lib);
            start(port, 20971520);
        } catch (Throwable ignored) {
        }
    }

    public native int start(int port, int size);

    public native int stop();
}
