package com.fongmi.android.tv.event;

import android.text.TextUtils;

public class PlayerEvent {

    private int state;
    private String msg;

    public PlayerEvent(int state) {
        this.state = state;
    }

    public PlayerEvent(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return TextUtils.isEmpty(msg) ? "" : msg;
    }

    public int getState() {
        return state;
    }
}
