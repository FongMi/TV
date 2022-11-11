package com.fongmi.android.tv.ui.custom;

import android.view.KeyEvent;

import com.fongmi.android.tv.utils.Utils;

public class CustomKeyDownVod {

    private final Listener listener;
    private int holdTime;

    public static CustomKeyDownVod create(Listener listener) {
        return new CustomKeyDownVod(listener);
    }

    private CustomKeyDownVod(Listener listener) {
        this.listener = listener;
    }

    public boolean onKeyDown(KeyEvent event) {
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
        return true;
    }

    public boolean hasEvent(KeyEvent event) {
        return Utils.isEnterKey(event) || Utils.isUpKey(event) || Utils.isDownKey(event) || Utils.isLeftKey(event) || Utils.isRightKey(event);
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
    }
}
