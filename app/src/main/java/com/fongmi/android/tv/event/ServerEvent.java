package com.fongmi.android.tv.event;

import org.greenrobot.eventbus.EventBus;

public class ServerEvent {

    private final Type type;
    private final String text;
    private final String name;

    public static void search(String text) {
        EventBus.getDefault().post(new ServerEvent(Type.SEARCH, text));
    }

    public static void push(String text) {
        EventBus.getDefault().post(new ServerEvent(Type.PUSH, text));
    }

    public static void setting(String text) {
        EventBus.getDefault().post(new ServerEvent(Type.SETTING, text));
    }

    public static void setting(String text, String name) {
        EventBus.getDefault().post(new ServerEvent(Type.SETTING, text, name));
    }

    private ServerEvent(Type type, String text) {
        this(type, text, "");
    }

    private ServerEvent(Type type, String text, String name) {
        this.type = type;
        this.text = text;
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String getName() {
        return name;
    }

    public enum Type {
        SEARCH, PUSH, SETTING
    }
}
