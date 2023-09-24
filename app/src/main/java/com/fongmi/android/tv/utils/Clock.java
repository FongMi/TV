package com.fongmi.android.tv.utils;

import android.widget.TextView;

import com.fongmi.android.tv.App;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Clock {

    private SimpleDateFormat format;
    private Callback callback;
    private final Date date;
    private TextView view;
    private Timer timer;

    public static Clock create() {
        return new Clock();
    }

    public static Clock create(TextView view) {
        return new Clock().view(view).format("HH:mm:ss");
    }

    public Clock() {
        this.date = new Date();
    }

    public Clock view(TextView view) {
        this.view = view;
        return this;
    }

    public Clock format(String format) {
        this.format = new SimpleDateFormat(format, Locale.getDefault());
        return this;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void start() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                App.post(() -> doJob());
            }
        }, 0, 1000);
    }

    private void doJob() {
        try {
            date.setTime(System.currentTimeMillis());
            if (callback != null) callback.onTimeChanged();
            if (view != null) view.setText(format.format(date));
        } catch (Exception ignored) {
        }
    }

    public Clock stop() {
        if (timer != null) timer.cancel();
        return this;
    }

    public void release() {
        if (timer != null) timer.cancel();
        if (callback != null) callback = null;
    }

    public interface Callback {

        void onTimeChanged();
    }
}
