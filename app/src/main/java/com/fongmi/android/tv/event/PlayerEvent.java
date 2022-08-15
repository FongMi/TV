package com.fongmi.android.tv.event;

import android.text.TextUtils;

import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.EventBus;

public class PlayerEvent {

    private final int state;
    private String msg;

    public static void error(int resId) {
        EventBus.getDefault().post(new PlayerEvent(ResUtil.getString(resId)));
    }

    public static void state(int state) {
        EventBus.getDefault().post(new PlayerEvent(state));
    }

    private PlayerEvent(int state) {
        this.state = state;
    }

    private PlayerEvent(String msg) {
        this.state = -1;
        this.msg = msg;
    }

    public String getMsg() {
        return TextUtils.isEmpty(msg) ? "" : msg;
    }

    public int getState() {
        return state;
    }
}
