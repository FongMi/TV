package com.fongmi.android.tv.utils;

import android.os.CountDownTimer;

import com.fongmi.android.tv.event.ActionEvent;

import java.util.concurrent.TimeUnit;

public class Timer {

    private CountDownTimer timer;
    private Callback callback;
    private long tick;

    private static class Loader {
        static volatile Timer INSTANCE = new Timer();
    }

    public static Timer get() {
        return Loader.INSTANCE;
    }

    public boolean isRunning() {
        return timer != null;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public long getTick() {
        return tick;
    }

    public void set(long t) {
        timer = new CountDownTimer(t, 1000) {
            @Override
            public void onTick(long tick) {
                Timer.this.onTick(tick);
            }

            @Override
            public void onFinish() {
                Timer.this.onFinish();
            }
        }.start();
    }

    private void onTick(long tick) {
        this.tick = tick;
        if (callback != null) callback.onTick(tick);
    }

    private void onFinish() {
        if (callback != null) callback.onFinish();
        ActionEvent.pause();
        reset();
    }

    public void delay() {
        cancel();
        set(TimeUnit.MINUTES.toMillis(5) + tick);
    }

    public void reset() {
        tick = 0;
        cancel();
    }

    public void cancel() {
        if (timer != null) timer.cancel();
        timer = null;
    }

    public interface Callback {

        void onTick(long tick);

        void onFinish();
    }
}
