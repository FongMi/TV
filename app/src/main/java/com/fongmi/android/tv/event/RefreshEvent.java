package com.fongmi.android.tv.event;

import org.greenrobot.eventbus.EventBus;

public class RefreshEvent {

    private final Type type;
    private String path;

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

    public static void detail() {
        EventBus.getDefault().post(new RefreshEvent(Type.DETAIL));
    }

    public static void player() {
        EventBus.getDefault().post(new RefreshEvent(Type.PLAYER));
    }

    public static void subtitle(String path) {
        EventBus.getDefault().post(new RefreshEvent(Type.SUBTITLE, path));
    }

    public static void danmaku(String path) {
        EventBus.getDefault().post(new RefreshEvent(Type.DANMAKU, path));
    }

    private RefreshEvent(Type type) {
        this.type = type;
    }

    public RefreshEvent(Type type, String path) {
        this.type = type;
        this.path = path;
    }

    public Type getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public enum Type {
        EMPTY, CONFIG, IMAGE, VIDEO, HISTORY, KEEP, SIZE, WALL, DETAIL, PLAYER, SUBTITLE, DANMAKU
    }
}
