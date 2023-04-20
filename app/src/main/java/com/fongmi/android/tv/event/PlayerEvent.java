package com.fongmi.android.tv.event;

import androidx.media3.common.Player;

import org.greenrobot.eventbus.EventBus;

public class PlayerEvent {

    private final int state;
    private final String url;

    public static void ready() {
        EventBus.getDefault().post(new PlayerEvent(Player.STATE_READY));
    }

    public static void state(int state) {
        EventBus.getDefault().post(new PlayerEvent(state));
    }

    public static void url(String url) {
        EventBus.getDefault().post(new PlayerEvent(url));
    }

    private PlayerEvent(int state) {
        this.state = state;
        this.url = "";
    }

    public PlayerEvent(String url) {
        this.state = 0;
        this.url = url;
    }

    public int getState() {
        return state;
    }

    public String getUrl() {
        return url;
    }
}
