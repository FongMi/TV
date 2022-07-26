package com.fongmi.bear.utils;

import android.view.KeyEvent;

import com.fongmi.bear.impl.KeyDownImpl;

public class KeyDown {

    private final KeyDownImpl mKeyDown;
    private int mHoldTime;

    public static KeyDown create(KeyDownImpl keyDown) {
        return new KeyDown(keyDown);
    }

    private KeyDown(KeyDownImpl keyDown) {
        this.mKeyDown = keyDown;
    }

    public boolean onKeyDown(KeyEvent event) {
        boolean isLeft = isLeftKey(event);
        boolean isRight = isRightKey(event);
        if (event.getAction() == KeyEvent.ACTION_DOWN && (isLeft || isRight)) {
            mKeyDown.onSeeking(isRight ? addTime() : subTime());
        } else if (event.getAction() == KeyEvent.ACTION_UP && (isLeft || isRight)) {
            mKeyDown.onSeekTo(mHoldTime);
        } else if (event.getAction() == KeyEvent.ACTION_UP && isDownKey(event)) {
            mKeyDown.onKeyDown();
        } else if (event.getAction() == KeyEvent.ACTION_UP && isEnterKey(event)) {
            mKeyDown.onKeyCenter();
        }
        return true;
    }

    private int addTime() {
        return mHoldTime = mHoldTime + 10000;
    }

    private int subTime() {
        return mHoldTime = mHoldTime - 10000;
    }

    public void resetTime() {
        mHoldTime = 0;
    }

    public boolean hasEvent(KeyEvent event) {
        return isEnterKey(event) || isUpKey(event) || isDownKey(event) || isLeftKey(event) || isRightKey(event);
    }

    private boolean isEnterKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER || event.getKeyCode() == KeyEvent.KEYCODE_SPACE || event.getKeyCode() == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }

    private boolean isUpKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP || event.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_UP || event.getKeyCode() == KeyEvent.KEYCODE_PAGE_UP || event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PREVIOUS;
    }

    private boolean isDownKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_CHANNEL_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_PAGE_DOWN || event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_NEXT;
    }

    private boolean isLeftKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT;
    }

    private boolean isRightKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_DPAD_RIGHT;
    }
}
