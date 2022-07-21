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
        if (event.getAction() == KeyEvent.ACTION_DOWN && Utils.isLeftKey(event)) {
            mKeyDown.onSeek(false);
        } else if (event.getAction() == KeyEvent.ACTION_DOWN && Utils.isRightKey(event)) {
            mKeyDown.onSeek(true);
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isUpKey(event)) {
            mKeyDown.onKeyUp();
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isDownKey(event)) {
            mKeyDown.onKeyDown();
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isLeftKey(event)) {
            mKeyDown.onKeyLeft();
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isRightKey(event)) {
            mKeyDown.onKeyRight();
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isBackKey(event)) {
            mKeyDown.onKeyBack();
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isMenuKey(event)) {
            mKeyDown.onKeyMenu();
        } else if (Utils.isEnterKey(event)) {
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
}
