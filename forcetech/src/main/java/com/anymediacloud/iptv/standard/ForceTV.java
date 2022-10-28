package com.anymediacloud.iptv.standard;

public class ForceTV {

    public void start(int port) {
        try {
            start(port, 20971520);
        } catch (Throwable ignored) {
        }
    }

    public native int start(int port, int size);

    public native int stop();
}
