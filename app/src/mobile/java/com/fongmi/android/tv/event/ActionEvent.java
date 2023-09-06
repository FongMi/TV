package com.fongmi.android.tv.event;

import org.greenrobot.eventbus.EventBus;

public class ActionEvent {

    public static String PREV = "PREV";
    public static String NEXT = "NEXT";
    public static String PLAY = "PLAY";
    public static String PAUSE = "PAUSE";
    public static String UPDATE = "UPDATE";
    public static String CANCEL = "CANCEL";

    private String type;

    public static void send(String action) {
        EventBus.getDefault().post(new ActionEvent(action));
    }

    public static void update() {
        EventBus.getDefault().post(new ActionEvent(UPDATE));
    }

    public ActionEvent(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
