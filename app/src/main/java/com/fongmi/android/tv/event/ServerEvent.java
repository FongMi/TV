package com.fongmi.android.tv.event;

import org.greenrobot.eventbus.EventBus;

public class ServerEvent {

    private final Type type;
    private String text;

    public static void search(String text) {
        EventBus.getDefault().post(new ServerEvent(Type.SEARCH, text));
    }

    public static void update(String text) {
        EventBus.getDefault().post(new ServerEvent(Type.UPDATE, text));
    }

    public static void push(String text) {
        EventBus.getDefault().post(new ServerEvent(Type.PUSH, text));
    }

    public static void api(String text) {
        EventBus.getDefault().post(new ServerEvent(Type.API, text));
    }

    public static void file() {
        EventBus.getDefault().post(new ServerEvent(Type.FILE));
    }

    public ServerEvent(Type type) {
        this.type = type;
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
        SEARCH, UPDATE, PUSH, API, FILE
    }
}
