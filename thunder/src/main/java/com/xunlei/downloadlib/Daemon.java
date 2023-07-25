package com.xunlei.downloadlib;

import android.os.Looper;

public class Daemon {
    private static volatile boolean shouldStop;
    private static Thread thread = null;
    private static Looper looper = null;

    public static synchronized void start() {
        if (thread != null) return;
        BlockingItem<Looper> bl = new BlockingItem<>();
        thread = new Thread(() -> {
            Looper.prepare();
            Looper l = Looper.myLooper();
            bl.put(l);
            while (!shouldStop) Looper.loop();
        }, "daemon");
        shouldStop = false;
        thread.start();
        try {
            looper = bl.take();
        } catch (InterruptedException ignored) {
        }
    }

    public static synchronized void stop() {
        shouldStop = true;
        if (thread != null && looper != null) {
            looper.quit();
            try {
                thread.join();
            } catch (Exception ignored) {
            }
            thread = null;
            looper = null;
        }
    }

    public static Looper looper() {
        if (looper == null) start();
        return looper == null ? Looper.getMainLooper() : looper;
    }
}
