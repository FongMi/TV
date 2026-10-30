package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.view.KeyEvent;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.utils.KeyUtil;

public abstract class SeekInput<T extends SeekInput.Listener> extends BaseInput<T> {

    private static final int SEEK_COMMIT_DELAY_MS = 250;

    private final Runnable seekCommit = this::commitSeek;
    private int seekKeyCode = KeyEvent.KEYCODE_UNKNOWN;
    private int seekKeyDeviceId;
    private long seekKeyDownTime;
    private boolean seekPending;

    protected SeekInput(Context context, T listener) {
        super(context, listener);
    }

    public final void cancelSeek() {
        seekKeyCode = KeyEvent.KEYCODE_UNKNOWN;
        cancelPendingSeek();
    }

    private void cancelPendingSeek() {
        boolean wasSeeking = seekPending;
        App.removeCallbacks(seekCommit);
        resetSeekTime();
        seekPending = false;
        if (wasSeeking) listener.onSeekCancel();
    }

    protected final boolean onSeekKey(KeyEvent event) {
        if (!KeyUtil.isLeftKey(event) && !KeyUtil.isRightKey(event)) return false;
        if (KeyUtil.isActionDown(event)) onSeekKeyDown(event);
        else if (KeyUtil.isActionUp(event)) onSeekKeyUp(event);
        return true;
    }

    private void onSeekKeyDown(KeyEvent event) {
        if (event.getRepeatCount() == 0) {
            seekKeyCode = event.getKeyCode();
            seekKeyDeviceId = event.getDeviceId();
            seekKeyDownTime = event.getDownTime();
        }
        if (!ownsSeekKey(event)) return;
        if (listener.canSeek()) {
            App.removeCallbacks(seekCommit);
            seekPending = true;
            listener.onSeeking(offsetSeekTime(KeyUtil.isLeftKey(event) ? -Constant.INTERVAL_SEEK : Constant.INTERVAL_SEEK));
        } else {
            cancelPendingSeek();
        }
    }

    private void onSeekKeyUp(KeyEvent event) {
        if (!ownsSeekKey(event)) return;
        seekKeyCode = KeyEvent.KEYCODE_UNKNOWN;
        if (event.isCanceled()) {
            cancelSeek();
        } else if (!listener.canSeek()) {
            cancelSeek();
            onSeekUnavailable(event);
        } else if (seekPending) {
            App.post(seekCommit, SEEK_COMMIT_DELAY_MS);
        }
    }

    private boolean ownsSeekKey(KeyEvent event) {
        return seekKeyCode == event.getKeyCode() && seekKeyDeviceId == event.getDeviceId() && seekKeyDownTime == event.getDownTime();
    }

    private void commitSeek() {
        if (!listener.canSeek()) {
            cancelSeek();
            return;
        }
        long time = consumeSeekTime();
        seekPending = false;
        listener.onSeekEnd(time);
    }

    protected void onSeekUnavailable(KeyEvent event) {
    }

    public interface Listener extends BaseInput.Listener {

        default boolean canSeek() {
            return true;
        }

        void onSeeking(long time);

        void onSeekEnd(long time);

        void onSeekCancel();
    }
}
