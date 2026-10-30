package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.view.KeyEvent;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.utils.KeyUtil;

public final class LiveInput extends BaseInput<LiveInput.Listener> {

    private static final int CHANNEL_NUMBER_TIMEOUT_MS = 2000;
    private static final int SEEK_COMMIT_DELAY_MS = 250;

    private final StringBuilder channelNumber;

    private final Runnable findChannel = this::findChannel;
    private final Runnable seekCommit = this::commitSeek;

    public static LiveInput create(Context context, Listener listener) {
        return new LiveInput(context, listener);
    }

    private LiveInput(Context context, Listener listener) {
        super(context, listener);
        this.channelNumber = new StringBuilder();
    }

    public void cancelSeek() {
        App.removeCallbacks(seekCommit);
        resetSeekTime();
    }

    private void commitSeek() {
        long time = consumeSeekTime();
        if (listener.canSeek()) listener.onSeekEnd(time);
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
        boolean seekKey = KeyUtil.isLeftKey(event) || KeyUtil.isRightKey(event);
        if (KeyUtil.isActionDown(event) && seekKey) {
            App.removeCallbacks(seekCommit);
            if (listener.canSeek()) listener.onSeeking(offsetSeekTime(KeyUtil.isLeftKey(event) ? -Constant.INTERVAL_SEEK : Constant.INTERVAL_SEEK));
            else cancelSeek();
        } else if (KeyUtil.isActionDown(event) && KeyUtil.isUpKey(event)) {
            listener.onKeyUp();
        } else if (KeyUtil.isActionDown(event) && KeyUtil.isDownKey(event)) {
            listener.onKeyDown();
        } else if (KeyUtil.isActionUp(event) && seekKey) {
            if (listener.canSeek()) App.post(seekCommit, SEEK_COMMIT_DELAY_MS);
            else {
                cancelSeek();
                if (KeyUtil.isLeftKey(event)) listener.onKeyLeft();
                else listener.onKeyRight();
            }
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

    private void findChannel() {
        listener.onFind(channelNumber.toString());
        channelNumber.setLength(0);
    }

    private int getNumber(int keyCode) {
        return keyCode >= KeyEvent.KEYCODE_NUMPAD_0 ? keyCode - KeyEvent.KEYCODE_NUMPAD_0 : keyCode - KeyEvent.KEYCODE_0;
    }

    public interface Listener extends BaseInput.Listener {

        boolean canHandleKeyEvent();

        boolean canSeek();

        void onShow(String number);

        void onFind(String number);

        void onSeeking(long time);

        void onSeekEnd(long time);

        void onKeyUp();

        void onKeyDown();

        void onKeyLeft();

        void onKeyRight();

        void onKeyCenter();

        void onMenu();
    }
}
