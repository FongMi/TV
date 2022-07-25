package com.fongmi.bear.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Clock {

    private final SimpleDateFormat formatter;
    private final Handler handler;
    private Timer timer;

    private static class Loader {
        static volatile Clock INSTANCE = new Clock();
    }

    public static Clock get() {
        return Loader.INSTANCE;
    }

    public Clock() {
        this.formatter = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault());
        this.handler = new Handler(Looper.getMainLooper());
    }

    public static void start(TextView view) {
        get().cancel();
        get().run(view);
    }

    public static void destroy() {
        get().cancel();
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

    private void cancel() {
        if (timer != null) timer.cancel();
    }
}
