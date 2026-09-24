package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.utils.KeyUtil;

public final class VodInput extends BaseInput<VodInput.Listener> {

    private boolean changeSpeed;
    private boolean fullscreen;

    public static VodInput create(Context context, Listener listener) {
        return new VodInput(context, listener);
    }

    private VodInput(Context context, Listener listener) {
        super(context, listener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return fullscreen && super.onTouchEvent(event);
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public boolean onKeyEvent(KeyEvent event) {
        if (!isSupportedKey(event)) return false;
        handleKeyEvent(event);
        return true;
    }

    private boolean isSupportedKey(KeyEvent event) {
        return KeyUtil.isEnterKey(event) || KeyUtil.isUpKey(event) || KeyUtil.isDownKey(event) || KeyUtil.isLeftKey(event) || KeyUtil.isRightKey(event);
    }

    private void handleKeyEvent(KeyEvent event) {
        if (KeyUtil.isActionDown(event) && KeyUtil.isLeftKey(event)) {
            listener.onSeeking(offsetSeekTime(-Constant.INTERVAL_SEEK));
        } else if (KeyUtil.isActionDown(event) && KeyUtil.isRightKey(event)) {
            listener.onSeeking(offsetSeekTime(Constant.INTERVAL_SEEK));
        } else if (KeyUtil.isActionUp(event) && (KeyUtil.isLeftKey(event) || KeyUtil.isRightKey(event))) {
            App.post(() -> listener.onSeekEnd(getSeekTime()), 250);
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isUpKey(event)) {
            if (changeSpeed) listener.onSpeedEnd();
            else listener.onKeyUp();
            changeSpeed = false;
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isDownKey(event)) {
            listener.onKeyDown();
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isEnterKey(event)) {
            listener.onKeyCenter();
        } else if (event.isLongPress() && KeyUtil.isUpKey(event)) {
            listener.onSpeedUp();
            changeSpeed = true;
        }
    }

    public void reset() {
        resetSeekTime();
    }

    public interface Listener extends BaseInput.Listener {

        void onSeeking(long time);

        void onSeekEnd(long time);

        void onSpeedUp();

        void onSpeedEnd();

        void onKeyUp();

        void onKeyDown();

        void onKeyCenter();
    }
}
