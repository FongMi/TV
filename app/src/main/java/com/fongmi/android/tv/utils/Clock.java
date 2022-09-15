package com.fongmi.android.tv.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Clock {

    private SimpleDateFormat formatter;
    private Callback callback;
    private Handler handler;
    private Timer timer;
    private Date date;

    private static class Loader {
        static volatile Clock INSTANCE = new Clock();
    }

    public static Clock get() {
        return Loader.INSTANCE;
    }

    public void init() {
        this.formatter = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault());
        this.handler = new Handler(Looper.getMainLooper());
        this.date = new Date();
    }

    public static void start(TextView view) {
        get().init();
        get().run(view);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private void run(TextView view) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> doJob(view));
            }
        }, 0, 1000);
    }

    private void doJob(TextView view) {
        try {
            date.setTime(System.currentTimeMillis());
            view.setText(formatter.format(date));
            if (callback != null) callback.onTimeChanged();
        } catch (Exception ignored) {
        }
    }

    public void release() {
        if (timer != null) timer.cancel();
    }

    public interface Callback {

        void onTimeChanged();
    }
}
