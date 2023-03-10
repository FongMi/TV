package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.utils.ResUtil;

public class CustomKeyDownVod extends GestureDetector.SimpleOnGestureListener {

    private final GestureDetector detector;
    private final Listener listener;
    private Runnable runnable;
    private boolean touch;
    private boolean seek;
    private int time;

    public static CustomKeyDownVod create(Context context) {
        return new CustomKeyDownVod(context);
    }

    private CustomKeyDownVod(Context context) {
        this.listener = (Listener) context;
        this.detector = new GestureDetector(context, this);
    }

    public boolean onTouchEvent(MotionEvent e) {
        if (seek && e.getAction() == MotionEvent.ACTION_UP) seekDone();
        return detector.onTouchEvent(e);
    }

    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        int width = ResUtil.getScreenWidthPx();
        int edgeX = (int) Math.abs(e.getX() - width);
        touch = e.getX() > 200 && edgeX > 200;
        seek = false;
        return true;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent e) {
        int base = ResUtil.getScreenWidthPx() / 3;
        boolean left = e.getX() > 0 && e.getX() < base;
        boolean right = e.getX() > base * 2 && e.getX() < base * 3;
        if (left) App.post(runnable = this::subTime, 0);
        if (right) App.post(runnable = this::addTime, 0);
        seek = left || right;
    }

    @Override
    public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
        int deltaX = (int) (e2.getX() - e1.getX());
        if (touch) {
            seek = Math.abs(distanceX) >= Math.abs(distanceY);
            touch = false;
        }
        if (seek) {
            listener.onSeeking(time = deltaX * 50);
        }
        return true;
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent e) {
        listener.onDoubleTap();
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
        listener.onSingleTap();
        return true;
    }

    private void addTime() {
        listener.onSeeking(time = time + Constant.INTERVAL_SEEK);
        App.post(runnable, getDelay());
    }

    private void subTime() {
        listener.onSeeking(time = time - Constant.INTERVAL_SEEK);
        App.post(runnable, getDelay());
    }

    private int getDelay() {
        int count = Math.abs(time) / Constant.INTERVAL_SEEK;
        if (count < 5) return 250;
        else if (count < 15) return 100;
        else return 50;
    }

    private void seekDone() {
        App.removeCallbacks(runnable);
        listener.onSeekTo(time);
        seek = false;
        time = 0;
    }

    public interface Listener {

        void onSeeking(int time);

        void onSeekTo(int time);

        void onSingleTap();

        void onDoubleTap();
    }
}