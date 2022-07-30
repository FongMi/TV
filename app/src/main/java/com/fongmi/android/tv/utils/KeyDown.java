package com.fongmi.android.tv.utils;

import android.view.KeyEvent;

public class KeyDown {

    private final Listener mListener;
    private int mHoldTime;

    public static KeyDown create(Listener listener) {
        return new KeyDown(listener);
    }

    private KeyDown(Listener listener) {
        this.mListener = listener;
    }

    public boolean onKeyDown(KeyEvent event) {
        boolean isLeft = isLeftKey(event);
        boolean isRight = isRightKey(event);
        if (event.getAction() == KeyEvent.ACTION_DOWN && (isLeft || isRight)) {
            mListener.onSeeking(isRight ? addTime() : subTime());
        } else if (event.getAction() == KeyEvent.ACTION_UP && (isLeft || isRight)) {
            mListener.onSeekTo(mHoldTime);
        } else if (event.getAction() == KeyEvent.ACTION_UP && isDownKey(event)) {
            mListener.onKeyDown();
        } else if (event.getAction() == KeyEvent.ACTION_UP && isEnterKey(event)) {
            mListener.onKeyCenter();
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

    public interface Listener {

        void onSeeking(int time);

        void onSeekTo(int time);

        void onKeyDown();

        void onKeyCenter();
    }
}
