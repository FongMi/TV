package com.fongmi.android.tv.utils;

import android.widget.TextView;

import com.fongmi.android.tv.App;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Clock {

    private SimpleDateFormat formatter;
    private Callback callback;
    private Timer timer;
    private Date date;

    private static class Loader {
        static volatile Clock INSTANCE = new Clock();
    }

    public static Clock get() {
        return Loader.INSTANCE;
    }

    public void init(String format) {
        this.formatter = new SimpleDateFormat(format, Locale.getDefault());
        this.date = new Date();
    }

    public static void stop() {
        get().release();
    }

    public static void start() {
        start(null);
    }

    public static void start(TextView view) {
        start(view, "HH:mm:ss");
    }

    public static void start(TextView view, String format) {
        get().init(format);
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
                App.post(() -> doJob(view));
            }
        }, 0, 1000);
    }

    private void doJob(TextView view) {
        try {
            date.setTime(System.currentTimeMillis());
            if (callback != null) callback.onTimeChanged();
            if (view != null) view.setText(formatter.format(date));
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
