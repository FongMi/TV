package com.fongmi.android.tv.event;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.EventBus;

public class ErrorEvent {

    private final String tag;
    private final Type type;
    private String msg;

    public static void url(String tag) {
        EventBus.getDefault().post(new ErrorEvent(tag, Type.URL));
    }

    public static void drm(String tag) {
        EventBus.getDefault().post(new ErrorEvent(tag, Type.DRM));
    }

    public static void flag(String tag) {
        EventBus.getDefault().post(new ErrorEvent(tag, Type.FLAG));
    }

    public static void parse(String tag) {
        EventBus.getDefault().post(new ErrorEvent(tag, Type.PARSE));
    }

    public static void timeout(String tag) {
        EventBus.getDefault().post(new ErrorEvent(tag, Type.TIMEOUT));
    }

    public static void extract(String tag, String msg) {
        EventBus.getDefault().post(new ErrorEvent(tag, Type.EXTRACT, msg));
    }

    public ErrorEvent(String tag, Type type) {
        this.type = type;
        this.tag = tag;
    }

    public ErrorEvent(String tag, Type type, String msg) {
        this.type = type;
        this.tag = tag;
        this.msg = msg;
    }

    public Type getType() {
        return type;
    }

    public String getTag() {
        return tag;
    }

    public String getMsg() {
        if (type == Type.URL) return ResUtil.getString(R.string.error_play_url);
        if (type == Type.DRM) return ResUtil.getString(R.string.error_play_drm);
        if (type == Type.FLAG) return ResUtil.getString(R.string.error_play_flag);
        if (type == Type.PARSE) return ResUtil.getString(R.string.error_play_parse);
        if (type == Type.TIMEOUT) return ResUtil.getString(R.string.error_play_timeout);
        return msg;
    }

    public enum Type {
        URL, DRM, FLAG, PARSE, TIMEOUT, EXTRACT
    }
}
