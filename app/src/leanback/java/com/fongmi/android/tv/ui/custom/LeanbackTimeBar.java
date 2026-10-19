package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.media3.ui.DefaultTimeBar;

import com.fongmi.android.tv.Constant;

public final class LeanbackTimeBar extends DefaultTimeBar {

    public LeanbackTimeBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isEnabled() && isSeekKey(keyCode)) updateKeyIncrement(event);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isSeekKey(keyCode)) resetKeyIncrement();
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus) resetKeyIncrement();
    }

    @Override
    protected void onDetachedFromWindow() {
        resetKeyIncrement();
        super.onDetachedFromWindow();
    }

    private boolean isSeekKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    private void updateKeyIncrement(KeyEvent event) {
        long heldMs = Math.max(0, event.getEventTime() - event.getDownTime());
        setKeyTimeIncrement(Constant.INTERVAL_SEEK * (1 + heldMs / 1000));
    }

    private void resetKeyIncrement() {
        setKeyTimeIncrement(Constant.INTERVAL_SEEK);
    }
}
