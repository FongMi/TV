package com.fongmi.android.tv.event;

public class ServerEvent {

    private final String text;
    private final Type type;

    public static ServerEvent search(String text) {
        return new ServerEvent(Type.SEARCH, text);
    }

    public static ServerEvent push(String text) {
        return new ServerEvent(Type.PUSH, text);
    }

    public static ServerEvent api(String text) {
        return new ServerEvent(Type.API, text);
    }

    public ServerEvent(Type type, String text) {
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
