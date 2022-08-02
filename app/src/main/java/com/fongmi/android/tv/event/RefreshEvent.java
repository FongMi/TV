package com.fongmi.android.tv.event;

public class RefreshEvent {

    private final Type type;

    public static RefreshEvent image() {
        return new RefreshEvent(Type.IMAGE);
    }

    public static RefreshEvent video() {
        return new RefreshEvent(Type.VIDEO);
    }

    public static RefreshEvent history() {
        return new RefreshEvent(Type.HISTORY);
    }

    public RefreshEvent(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        IMAGE, VIDEO, HISTORY
    }
}
