package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;

public abstract class BaseInput<T extends BaseInput.Listener> extends GestureDetector.SimpleOnGestureListener {

    public static final int SEEK_TAP_TIMEOUT_MS = 1000;

    private final GestureDetector detector;
    private final int touchSlopSquared;
    private final boolean doubleTapEnabled;
    protected final T listener;
    private boolean speedPressActive;
    private boolean tapMoved;
    private long seekTime;
    private long seekTapUntil;
    private long seekStartDownTime;
    private long currentDownTime;
    private float downX;
    private float downY;

    protected BaseInput(Context context, T listener) {
        this(context, listener, true);
    }

    protected BaseInput(Context context, T listener, boolean doubleTapEnabled) {
        this.detector = new GestureDetector(context, this);
        if (!doubleTapEnabled) detector.setOnDoubleTapListener(null);
        int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        this.touchSlopSquared = touchSlop * touchSlop;
        this.doubleTapEnabled = doubleTapEnabled;
        this.listener = listener;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            currentDownTime = event.getDownTime();
            downX = event.getX();
            downY = event.getY();
            tapMoved = false;
        } else if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_CANCEL) {
            tapMoved = true;
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dx = event.getX() - downX;
            float dy = event.getY() - downY;
            if (dx * dx + dy * dy > touchSlopSquared) tapMoved = true;
        }
        boolean handled = detector.onTouchEvent(event);
        if (action == MotionEvent.ACTION_UP && isAdditionalSeekTap(event)) {
            if (listener.onSeekTap(event.getX())) {
                seekTapUntil = event.getEventTime() + SEEK_TAP_TIMEOUT_MS;
                handled = true;
            } else cancelSeekTaps();
        }
        return handled;
    }

    public void cancelSeekTaps() {
        seekTapUntil = 0;
    }

    protected final boolean isSeekTapActive() {
        return isSeekTapActive(SystemClock.uptimeMillis());
    }

    private boolean isSeekTapActive(long time) {
        return time <= seekTapUntil;
    }

    private boolean isAdditionalSeekTap(MotionEvent event) {
        return currentDownTime != seekStartDownTime && isSeekTapActive(currentDownTime) && !tapMoved
                && event.getEventTime() - currentDownTime < ViewConfiguration.getLongPressTimeout() && acceptTap(event);
    }

    protected final long setSeekTime(long time) {
        return seekTime = time;
    }

    protected final long offsetSeekTime(long offset) {
        return seekTime += offset;
    }

    protected final long consumeSeekTime() {
        long time = seekTime;
        seekTime = 0;
        return time;
    }

    protected final void resetSeekTime() {
        seekTime = 0;
    }

    protected final void beginSpeedPress() {
        if (!speedPressActive) speedPressActive = onSpeedPressBegin();
    }

    protected final void finishSpeedPress() {
        if (!speedPressActive) return;
        speedPressActive = false;
        onSpeedPressFinish();
    }

    protected final boolean isSpeedPressActive() {
        return speedPressActive;
    }

    protected boolean onSpeedPressBegin() {
        return false;
    }

    protected void onSpeedPressFinish() {
    }

    protected boolean acceptTap(MotionEvent event) {
        return true;
    }

    protected void onDoubleTapHandled() {
    }

    @Override
    public boolean onDown(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public final boolean onDoubleTap(@NonNull MotionEvent event) {
        if (acceptTap(event)) {
            onDoubleTapHandled();
            if (isSeekTapActive(currentDownTime)) {
                if (!listener.isSeekTapArea(downX)) {
                    cancelSeekTaps();
                    listener.onDoubleTap();
                }
                return true;
            }
            if (listener.isSeekTapArea(downX)) {
                if (listener.onSeekTap(downX)) {
                    seekStartDownTime = currentDownTime;
                    seekTapUntil = currentDownTime + SEEK_TAP_TIMEOUT_MS;
                }
            } else listener.onDoubleTap();
        }
        return true;
    }

    @Override
    public final boolean onSingleTapUp(@NonNull MotionEvent event) {
        if (doubleTapEnabled) return false;
        if (acceptTap(event)) listener.onSingleTap();
        return true;
    }

    @Override
    public final boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
        if (doubleTapEnabled && acceptTap(event) && !isSeekTapActive()) listener.onSingleTap();
        return true;
    }

    public interface Listener {

        void onSingleTap();

        default void onDoubleTap() {
        }

        default boolean isSeekTapArea(float x) {
            return false;
        }

        default boolean onSeekTap(float x) {
            return false;
        }
    }
}
