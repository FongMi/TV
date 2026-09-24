package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

public abstract class BaseInput<T extends BaseInput.Listener> extends GestureDetector.SimpleOnGestureListener {

    protected final T listener;
    private final GestureDetector detector;
    private long seekTime;

    protected BaseInput(Context context, T listener) {
        this.detector = new GestureDetector(context, this);
        this.listener = listener;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return detector.onTouchEvent(event);
    }

    protected final long offsetSeekTime(long offset) {
        return seekTime += offset;
    }

    protected final long getSeekTime() {
        return seekTime;
    }

    protected final void resetSeekTime() {
        seekTime = 0;
    }

    @Override
    public boolean onDown(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent event) {
        listener.onDoubleTap();
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        listener.onSingleTap();
        return true;
    }

    public interface Listener {

        void onSingleTap();

        void onDoubleTap();
    }
}
