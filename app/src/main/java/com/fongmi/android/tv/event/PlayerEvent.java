package com.fongmi.android.tv.event;

import androidx.media3.common.Player;

import org.greenrobot.eventbus.EventBus;

public class PlayerEvent {

    private final int state;

    public static void ready() {
        EventBus.getDefault().post(new PlayerEvent(Player.STATE_READY));
    }

    public static void state(int state) {
        EventBus.getDefault().post(new PlayerEvent(state));
    }

    private PlayerEvent(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }
}
