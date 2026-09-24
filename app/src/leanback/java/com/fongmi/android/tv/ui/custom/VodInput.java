package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.utils.KeyUtil;

public final class VodInput extends BaseInput<VodInput.Listener> {

    private static final int SEEK_COMMIT_DELAY_MS = 250;

    private boolean fullscreen;

    public static VodInput create(Context context, Listener listener) {
        return new VodInput(context, listener);
    }

    private VodInput(Context context, Listener listener) {
        super(context, listener, false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!fullscreen) return false;
        return super.onTouchEvent(e);
    }

    public void setFullscreen(boolean fullscreen) {
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
        if (KeyUtil.isActionDown(event) && KeyUtil.isLeftKey(event)) {
            listener.onSeeking(offsetSeekTime(-Constant.INTERVAL_SEEK));
        } else if (KeyUtil.isActionDown(event) && KeyUtil.isRightKey(event)) {
            listener.onSeeking(offsetSeekTime(Constant.INTERVAL_SEEK));
        } else if (KeyUtil.isActionUp(event) && (KeyUtil.isLeftKey(event) || KeyUtil.isRightKey(event))) {
            long time = consumeSeekTime();
            App.post(() -> listener.onSeekEnd(time), SEEK_COMMIT_DELAY_MS);
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isUpKey(event)) {
            if (isSpeedPressActive()) finishSpeedPress();
            else listener.onKeyUp();
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isDownKey(event)) {
            listener.onKeyDown();
        } else if (KeyUtil.isActionUp(event) && KeyUtil.isEnterKey(event)) {
            listener.onKeyCenter();
        } else if (event.isLongPress() && KeyUtil.isUpKey(event)) {
            beginSpeedPress();
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
