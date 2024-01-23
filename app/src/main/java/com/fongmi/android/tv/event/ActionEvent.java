package com.fongmi.android.tv.event;

import com.fongmi.android.tv.BuildConfig;

import org.greenrobot.eventbus.EventBus;

public class ActionEvent {

    public static String STOP = BuildConfig.APPLICATION_ID.concat(".stop");
    public static String PREV = BuildConfig.APPLICATION_ID.concat(".prev");
    public static String NEXT = BuildConfig.APPLICATION_ID.concat(".next");
    public static String PLAY = BuildConfig.APPLICATION_ID.concat(".play");
    public static String PAUSE = BuildConfig.APPLICATION_ID.concat(".pause");
    public static String UPDATE = BuildConfig.APPLICATION_ID.concat(".update");

    private final String action;

    public static void send(String action) {
        EventBus.getDefault().post(new ActionEvent(action));
    }

    public static void update() {
        send(UPDATE);
    }

    public static void next() {
        send(NEXT);
    }

    public static void pause() {
        send(PAUSE);
    }

    public ActionEvent(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public boolean isUpdate() {
        return UPDATE.equals(getAction());
    }
}
