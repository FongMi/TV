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
    private Handler handler;
    private Timer timer;

    private static class Loader {
        static volatile Clock INSTANCE = new Clock();
    }

    public static Clock get() {
        return Loader.INSTANCE;
    }

    public void init() {
        this.formatter = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault());
        this.handler = new Handler(Looper.getMainLooper());
    }

    public static void start(TextView view) {
        get().init();
        get().run(view);
    }

    private void run(TextView view) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> view.setText(formatter.format(new Date())));
            }
        }, 0, 1000);
    }

    public void release() {
        if (timer != null) timer.cancel();
        formatter = null;
        handler = null;
        timer = null;
    }
}
