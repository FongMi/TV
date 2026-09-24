package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.view.KeyEvent;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.utils.KeyUtil;

public final class LiveInput extends BaseInput<LiveInput.Listener> {

    private static final int CHANNEL_NUMBER_TIMEOUT_MS = 2000;

    private final StringBuilder channelNumber;

    private final Runnable findChannel = new Runnable() {
        @Override
        public void run() {
            listener.onFind(channelNumber.toString());
            channelNumber.setLength(0);
        }
    };

    public static LiveInput create(Context context, Listener listener) {
        return new LiveInput(context, listener);
    }

    private LiveInput(Context context, Listener listener) {
        super(context, listener, false);
        this.channelNumber = new StringBuilder();
    }

    private boolean isSupportedKey(KeyEvent event) {
        return KeyUtil.isEnterKey(event) || KeyUtil.isUpKey(event) || KeyUtil.isDownKey(event) || KeyUtil.isLeftKey(event) || KeyUtil.isRightKey(event) || KeyUtil.isDigitKey(event) || KeyUtil.isMenuKey(event);
    }

    public boolean onKeyEvent(KeyEvent event) {
        if (!isSupportedKey(event) || !listener.canHandleKeyEvent()) return false;
        handleKeyEvent(event);
        return true;
    }

    private void handleKeyEvent(KeyEvent event) {
        if (KeyUtil.isActionDown(event) && KeyUtil.isLeftKey(event)) {
            listener.onSeeking(offsetSeekTime(-Constant.INTERVAL_SEEK));
        } else if (KeyUtil.isActionDown(event) && KeyUtil.isRightKey(event)) {
            listener.onSeeking(offsetSeekTime(Constant.INTERVAL_SEEK));
        } else if (KeyUtil.isActionDown(event) && KeyUtil.isUpKey(event)) {
            listener.onKeyUp();
        } else if (KeyUtil.isActionDown(event) && KeyUtil.isDownKey(event)) {
            listener.onKeyDown();
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isLeftKey(event)) {
            listener.onKeyLeft(consumeSeekTime());
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isRightKey(event)) {
            listener.onKeyRight(consumeSeekTime());
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isDigitKey(event)) {
            appendChannelDigit(event.getKeyCode());
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isEnterKey(event)) {
            listener.onKeyCenter();
        } else if (KeyUtil.isMenuKey(event) || (event.isLongPress() && KeyUtil.isEnterKey(event))) {
            listener.onMenu();
        }
    }

    private void appendChannelDigit(int keyCode) {
        if (channelNumber.length() >= 4) return;
        channelNumber.append(getNumber(keyCode));
        listener.onShow(channelNumber.toString());
        App.post(findChannel, CHANNEL_NUMBER_TIMEOUT_MS);
    }

    private int getNumber(int keyCode) {
        return keyCode >= KeyEvent.KEYCODE_NUMPAD_0 ? keyCode - KeyEvent.KEYCODE_NUMPAD_0 : keyCode - KeyEvent.KEYCODE_0;
    }

    public interface Listener extends BaseInput.Listener {

        boolean canHandleKeyEvent();

        void onShow(String number);

        void onFind(String number);

        void onSeeking(long time);

        void onKeyUp();

        void onKeyDown();

        void onKeyLeft(long time);

        void onKeyRight(long time);

        void onKeyCenter();

        void onMenu();
    }
}
