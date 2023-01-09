package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.utils.Utils;

public class CustomKeyDownVod extends GestureDetector.SimpleOnGestureListener {

    private final GestureDetector detector;
    private final Listener listener;
    private int holdTime;

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

    public boolean hasEvent(KeyEvent event) {
        return Utils.isEnterKey(event) || Utils.isUpKey(event) || Utils.isDownKey(event) || Utils.isLeftKey(event) || Utils.isRightKey(event);
    }

    public boolean onKeyDown(KeyEvent event) {
        check(event);
        return true;
    }

    private void check(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && Utils.isLeftKey(event)) {
            listener.onSeeking(subTime());
        } else if (event.getAction() == KeyEvent.ACTION_DOWN && Utils.isRightKey(event)) {
            listener.onSeeking(addTime());
        } else if (event.getAction() == KeyEvent.ACTION_UP && (Utils.isLeftKey(event) || Utils.isRightKey(event))) {
            listener.onSeekTo(holdTime);
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isUpKey(event)) {
            listener.onKeyUp();
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isDownKey(event)) {
            listener.onKeyDown();
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isEnterKey(event)) {
            listener.onKeyCenter();
        }
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

    private int addTime() {
        return holdTime = holdTime + 10000;
    }

    private int subTime() {
        return holdTime = holdTime - 10000;
    }

    public void resetTime() {
        holdTime = 0;
    }

    public interface Listener {

        void onSeeking(int time);

        void onSeekTo(int time);

        void onKeyUp();

        void onKeyDown();

        void onKeyCenter();

        void onSingleTap();

        void onDoubleTap();
    }
}
