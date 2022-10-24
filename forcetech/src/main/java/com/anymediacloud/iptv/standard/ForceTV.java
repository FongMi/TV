package com.anymediacloud.iptv.standard;

import com.gsoft.mitv.MainActivity;

public class ForceTV {

    static {
        MainActivity.start();
    }

    public void start(int port) {
        try {
            start(port, 20971520);
        } catch (Throwable ignored) {
        }
    }

    public native int start(int port, int size);

    public native int stop();
}
