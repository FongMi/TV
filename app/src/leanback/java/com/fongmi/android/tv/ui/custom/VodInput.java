package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.utils.KeyUtil;

public final class VodInput extends BaseInput<VodInput.Listener> {

    private static final int SEEK_COMMIT_DELAY_MS = 250;

    private final Runnable seekCommit = this::commitSeek;
    private boolean fullscreen;
    private int centerKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    private long centerKeyDownTime;

    public static VodInput create(Context context, Listener listener) {
        return new VodInput(context, listener);
    }

    private VodInput(Context context, Listener listener) {
        super(context, listener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!fullscreen) return false;
        return super.onTouchEvent(e);
    }

    public void setFullscreen(boolean fullscreen) {
        if (!fullscreen) cancelSeek();
        this.fullscreen = fullscreen;
    }

    public void cancelSeek() {
        App.removeCallbacks(seekCommit);
        resetSeekTime();
    }

    private void commitSeek() {
        listener.onSeekEnd(consumeSeekTime());
    }

    private boolean isSupportedKey(KeyEvent event) {
        return KeyUtil.isEnterKey(event) || KeyUtil.isUpKey(event) || KeyUtil.isDownKey(event) || KeyUtil.isLeftKey(event) || KeyUtil.isRightKey(event);
    }

    public boolean onKeyEvent(KeyEvent event) {
        if (!isSupportedKey(event)) return false;
        handleKeyEvent(event);
        return true;
    }

    private void handleKeyEvent(KeyEvent event) {
        boolean seekKey = KeyUtil.isLeftKey(event) || KeyUtil.isRightKey(event);
        if (KeyUtil.isActionDown(event) && seekKey) {
            App.removeCallbacks(seekCommit);
            listener.onSeeking(offsetSeekTime(KeyUtil.isLeftKey(event) ? -Constant.INTERVAL_SEEK : Constant.INTERVAL_SEEK));
        } else if (KeyUtil.isActionUp(event) && seekKey) {
            App.post(seekCommit, SEEK_COMMIT_DELAY_MS);
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isUpKey(event)) {
            if (isSpeedPressActive()) finishSpeedPress();
            else listener.onKeyUp();
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isDownKey(event)) {
            listener.onKeyDown();
        } else if (KeyUtil.isEnterKey(event)) {
            onCenterKey(event);
        } else if (event.isLongPress() && KeyUtil.isUpKey(event)) {
            beginSpeedPress();
        }
    }

    private void onCenterKey(KeyEvent event) {
        if (KeyUtil.isActionDown(event) && event.getRepeatCount() == 0) {
            centerKeyCode = event.getKeyCode();
            centerKeyDownTime = event.getDownTime();
        } else if (KeyUtil.isActionUp(event)) {
            boolean ownsPress = centerKeyCode == event.getKeyCode() && centerKeyDownTime == event.getDownTime();
            centerKeyCode = KeyEvent.KEYCODE_UNKNOWN;
            if (ownsPress && !event.isCanceled()) listener.onKeyCenter();
        }
    }

    @Override
    protected boolean onSpeedPressBegin() {
        return listener.onSpeedPressStart();
    }

    @Override
    protected void onSpeedPressFinish() {
        listener.onSpeedPressEnd();
    }

    public interface Listener extends BaseInput.Listener {

        void onSeeking(long time);

        void onSeekEnd(long time);

        boolean onSpeedPressStart();

        void onSpeedPressEnd();

        void onKeyUp();

        void onKeyDown();

        void onKeyCenter();
    }
}
