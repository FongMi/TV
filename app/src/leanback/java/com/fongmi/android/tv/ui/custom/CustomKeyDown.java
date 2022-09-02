package com.fongmi.android.tv.ui.custom;

import android.view.KeyEvent;

import com.fongmi.android.tv.utils.Utils;

public class CustomKeyDown {

    private final Listener mListener;
    private int holdTime;

    public static CustomKeyDown create(Listener listener) {
        return new CustomKeyDown(listener);
    }

    private CustomKeyDown(Listener listener) {
        this.mListener = listener;
    }

    public boolean onKeyDown(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && (Utils.isLeftKey(event) || Utils.isRightKey(event))) {
            mListener.onSeeking(Utils.isRightKey(event) ? addTime() : subTime());
        } else if (event.getAction() == KeyEvent.ACTION_UP && (Utils.isLeftKey(event) || Utils.isRightKey(event))) {
            mListener.onSeekTo(holdTime);
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isDownKey(event)) {
            mListener.onKeyDown();
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isEnterKey(event)) {
            mListener.onKeyCenter();
        }
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

    public boolean hasEvent(KeyEvent event) {
        return Utils.isEnterKey(event) || Utils.isUpKey(event) || Utils.isDownKey(event) || Utils.isLeftKey(event) || Utils.isRightKey(event);
    }

    public interface Listener {

        void onSeeking(int time);

        void onSeekTo(int time);

        void onKeyDown();

        void onKeyCenter();
    }
}
