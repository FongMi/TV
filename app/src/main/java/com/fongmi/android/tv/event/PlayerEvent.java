package com.fongmi.android.tv.event;

import android.text.TextUtils;

import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.EventBus;

public class PlayerEvent {

    private final int state;
    private boolean retry;
    private String msg;

    public static void error(int resId) {
        EventBus.getDefault().post(new PlayerEvent(ResUtil.getString(resId), false));
    }

    public static void error(int resId, boolean retry) {
        EventBus.getDefault().post(new PlayerEvent(ResUtil.getString(resId), retry));
    }

    public static void state(int state) {
        EventBus.getDefault().post(new PlayerEvent(state));
    }

    private PlayerEvent(int state) {
        this.state = state;
    }

    private PlayerEvent(String msg, boolean retry) {
        this.msg = msg;
        this.retry = retry;
        this.state = -1;
    }

    public String getMsg() {
        return TextUtils.isEmpty(msg) ? "" : msg;
    }

    public int getState() {
        return state;
    }

    public boolean isRetry() {
        return retry;
    }
}
