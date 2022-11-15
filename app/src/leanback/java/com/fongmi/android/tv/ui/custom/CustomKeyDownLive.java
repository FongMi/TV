package com.fongmi.android.tv.ui.custom;

import android.view.KeyEvent;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.Utils;

public class CustomKeyDownLive {

    private final Listener listener;
    private final StringBuilder text;
    private int holdTime;

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            listener.onFind(text.toString());
            text.setLength(0);
        }
    };

    public static CustomKeyDownLive create(Listener listener) {
        return new CustomKeyDownLive(listener);
    }

    private CustomKeyDownLive(Listener listener) {
        this.listener = listener;
        this.text = new StringBuilder();
    }

    public void onKeyDown(int keyCode) {
        if (text.length() >= 4) return;
        text.append(getNumber(keyCode));
        listener.onShow(text.toString());
        App.post(runnable, 1000);
    }

    public boolean onKeyDown(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && Utils.isLeftKey(event)) {
            listener.onSeeking(subTime());
        } else if (event.getAction() == KeyEvent.ACTION_DOWN && Utils.isRightKey(event)) {
            listener.onSeeking(addTime());
        } else if (event.getAction() == KeyEvent.ACTION_DOWN && Utils.isUpKey(event)) {
            listener.onKeyUp();
        } else if (event.getAction() == KeyEvent.ACTION_DOWN && Utils.isDownKey(event)) {
            listener.onKeyDown();
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isLeftKey(event)) {
            listener.onKeyLeft(holdTime);
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isRightKey(event)) {
            listener.onKeyRight(holdTime);
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isDigitKey(event)) {
            onKeyDown(event.getKeyCode());
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isEnterKey(event)) {
            listener.onKeyCenter();
        } else if (event.isLongPress() && Utils.isEnterKey(event)) {
            listener.onLongPress();
        }
        return true;
    }

    public boolean hasEvent(KeyEvent event) {
        return Utils.isEnterKey(event) || Utils.isUpKey(event) || Utils.isDownKey(event) || Utils.isLeftKey(event) || Utils.isRightKey(event) || Utils.isDigitKey(event) || event.isLongPress();
    }

    private int getNumber(int keyCode) {
        return keyCode >= 144 ? keyCode - 144 : keyCode - 7;
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

        void onShow(String number);

        void onFind(String number);

        void onSeeking(int time);

        void onKeyUp();

        void onKeyDown();

        void onKeyLeft(int time);

        void onKeyRight(int time);

        void onKeyCenter();

        boolean onLongPress();
    }
}
