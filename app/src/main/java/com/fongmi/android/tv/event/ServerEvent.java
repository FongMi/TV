package com.fongmi.android.tv.event;

import org.greenrobot.eventbus.EventBus;

public class ServerEvent {

    private final Type type;
    private final String text;

    public static void search(String text) {
        EventBus.getDefault().post(new ServerEvent(Type.SEARCH, text));
    }

    public static void push(String text) {
        EventBus.getDefault().post(new ServerEvent(Type.PUSH, text));
    }

    public static void api(String text) {
        EventBus.getDefault().post(new ServerEvent(Type.API, text));
    }

    private ServerEvent(Type type, String text) {
        this.type = type;
        this.text = text;
    }

    public Type getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public enum Type {
        SEARCH, PUSH, API
    }
}
