package com.fongmi.android.tv.event;

import org.greenrobot.eventbus.EventBus;

public class RefreshEvent {

    private final Type type;

    public static void empty() {
        EventBus.getDefault().post(new RefreshEvent(Type.EMPTY));
    }

    public static void config() {
        EventBus.getDefault().post(new RefreshEvent(Type.CONFIG));
    }

    public static void image() {
        EventBus.getDefault().post(new RefreshEvent(Type.IMAGE));
    }

    public static void video() {
        EventBus.getDefault().post(new RefreshEvent(Type.VIDEO));
    }

    public static void history() {
        EventBus.getDefault().post(new RefreshEvent(Type.HISTORY));
    }

    public static void keep() {
        EventBus.getDefault().post(new RefreshEvent(Type.KEEP));
    }

    public static void size() {
        EventBus.getDefault().post(new RefreshEvent(Type.SIZE));
    }

    public static void wall() {
        EventBus.getDefault().post(new RefreshEvent(Type.WALL));
    }

    private RefreshEvent(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        EMPTY, CONFIG, IMAGE, VIDEO, HISTORY, KEEP, SIZE, WALL
    }
}
