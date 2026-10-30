package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.fongmi.android.tv.utils.KeyUtil;

public final class VodInput extends SeekInput<VodInput.Listener> {

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

    private boolean isSupportedKey(KeyEvent event) {
        return KeyUtil.isEnterKey(event) || KeyUtil.isUpKey(event) || KeyUtil.isDownKey(event) || KeyUtil.isLeftKey(event) || KeyUtil.isRightKey(event);
    }

    public boolean onKeyEvent(KeyEvent event) {
        if (!isSupportedKey(event)) return false;
        handleKeyEvent(event);
        return true;
    }

    private void handleKeyEvent(KeyEvent event) {
        if (onSeekKey(event)) return;
        if (KeyUtil.isActionUp(event) && KeyUtil.isUpKey(event)) {
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

    public interface Listener extends SeekInput.Listener {

        boolean onSpeedPressStart();

        void onSpeedPressEnd();

        void onKeyUp();

        void onKeyDown();

        void onKeyCenter();
    }
}
