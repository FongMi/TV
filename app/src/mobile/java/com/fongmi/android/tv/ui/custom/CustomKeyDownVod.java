package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.utils.ResUtil;

public class CustomKeyDownVod extends GestureDetector.SimpleOnGestureListener {

    private final GestureDetector detector;
    private final Listener listener;

    public static CustomKeyDownVod create(Context context) {
        return new CustomKeyDownVod(context);
    }

    private CustomKeyDownVod(Context context) {
        this.listener = (Listener) context;
        this.detector = new GestureDetector(context, this);
    }

    public boolean onTouchEvent(MotionEvent e) {
        return detector.onTouchEvent(e);
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent e) {
        int base = ResUtil.getScreenWidthPx() / 3;
        boolean left = e.getX() > 0 && e.getX() < base;
        boolean center = e.getX() > base && e.getX() < base * 2;
        boolean right = e.getX() > base * 2 && e.getX() < base * 3;
        if (left) listener.onDoubleTapLeft();
        if (right) listener.onDoubleTapRight();
        if (center) listener.onDoubleTapCenter();
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
        listener.onSingleTap();
        return true;
    }

    public interface Listener {

        void onSingleTap();

        void onDoubleTapLeft();

        void onDoubleTapRight();

        void onDoubleTapCenter();
    }
}