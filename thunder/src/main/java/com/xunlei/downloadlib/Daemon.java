package com.xunlei.downloadlib;

import android.os.Looper;

/**
 * 常驻的后台线程，用于处理一个消息循环
 * <p/>
 * 一般用于处理运算密集型任务、磁盘IO任务等执行时间小于1秒的任务
 *
 */
public class Daemon {
    private static volatile boolean shouldStop;
    private static Thread thread = null;
    private static Looper looper = null;

    public static synchronized void start() {
        if (thread == null) {
            final BlockingItem<Looper> bl = new BlockingItem<Looper>();
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    Looper l = Looper.myLooper();
                    bl.put(l);

                    while (!shouldStop) {
                        try {
                            Looper.loop();
                        } catch (Exception e) {
                        }
                    }
                }
            }, "daemon");

            shouldStop = false;
            thread.start();
            try {
                looper = bl.take();
            } catch (InterruptedException e) {
            }
        }
    }

    public static synchronized void stop() {
        shouldStop = true;

        if (thread != null && looper != null) {
            looper.quit();
            try {
                thread.join();
            } catch (Exception e) {
            }
            thread = null;
            looper = null;
        }
    }

    public static Looper looper() {
        if (looper == null) {
            start();
        }
        return looper == null ? Looper.getMainLooper() : looper;
    }
}
