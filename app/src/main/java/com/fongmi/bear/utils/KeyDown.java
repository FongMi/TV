package com.fongmi.bear.utils;

import android.view.KeyEvent;

import com.fongmi.bear.impl.KeyDownImpl;

public class KeyDown {

    private final KeyDownImpl mKeyDown;
    private boolean mPress;

    public static KeyDown create(KeyDownImpl keyDown) {
        return new KeyDown(keyDown);
    }

    private KeyDown(KeyDownImpl keyDown) {
        this.mKeyDown = keyDown;
    }

    public boolean onKeyDown(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && isLeftKey(event)) {
            mKeyDown.onSeek(false);
        } else if (event.getAction() == KeyEvent.ACTION_DOWN && isRightKey(event)) {
            mKeyDown.onSeek(true);
        } else if (event.getAction() == KeyEvent.ACTION_UP && isUpKey(event)) {
            mKeyDown.onKeyUp();
        } else if (event.getAction() == KeyEvent.ACTION_UP && isDownKey(event)) {
            mKeyDown.onKeyDown();
        } else if (event.getAction() == KeyEvent.ACTION_UP && isLeftKey(event)) {
            mKeyDown.onKeyLeft();
        } else if (event.getAction() == KeyEvent.ACTION_UP && isRightKey(event)) {
            mKeyDown.onKeyRight();
        } else if (event.getAction() == KeyEvent.ACTION_UP && isBackKey(event)) {
            mKeyDown.onKeyBack();
        } else if (event.getAction() == KeyEvent.ACTION_UP && isMenuKey(event)) {
            mKeyDown.onKeyMenu();
        } else if (isEnterKey(event)) {
            checkPress(event);
        }
        return true;
    }

    private void checkPress(KeyEvent event) {
        if (event.isLongPress()) {
            mPress = true;
            mKeyDown.onLongPress();
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            if (mPress) mPress = false;
            else mKeyDown.onKeyCenter();
        }
    }

    public boolean hasEvent(KeyEvent event) {
        return isArrowKey(event) || isBackKey(event) || isMenuKey(event) || isDigitKey(event) || event.isLongPress();
    }

    private boolean isArrowKey(KeyEvent event) {
        return isEnterKey(event) || isUpKey(event) || isDownKey(event) || isLeftKey(event) || isRightKey(event);
    }

    private boolean isBackKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_BACK;
    }

    private boolean isMenuKey(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_MENU;
    }

    private boolean isDigitKey(KeyEvent event) {
        return event.getKeyCode() >= KeyEvent.KEYCODE_0 && event.getKeyCode() <= KeyEvent.KEYCODE_9 || event.getKeyCode() >= KeyEvent.KEYCODE_NUMPAD_0 && event.getKeyCode() <= KeyEvent.KEYCODE_NUMPAD_9;
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
