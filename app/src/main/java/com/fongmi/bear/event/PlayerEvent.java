package com.fongmi.bear.event;

public class PlayerEvent {

    private final int state;

    public PlayerEvent(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }
}
