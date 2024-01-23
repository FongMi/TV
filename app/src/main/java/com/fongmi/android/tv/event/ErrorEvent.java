package com.fongmi.android.tv.event;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.EventBus;

public class ErrorEvent {

    private final Type type;
    private final int retry;
    private String msg;

    public static void url(int retry) {
        EventBus.getDefault().post(new ErrorEvent(Type.URL, retry));
    }

    public static void flag() {
        EventBus.getDefault().post(new ErrorEvent(Type.FLAG, 0));
    }

    public static void parse() {
        EventBus.getDefault().post(new ErrorEvent(Type.PARSE, 0));
    }

    public static void timeout() {
        EventBus.getDefault().post(new ErrorEvent(Type.TIMEOUT, 0));
    }

    public static void extract(String msg) {
        EventBus.getDefault().post(new ErrorEvent(Type.EXTRACT, 0, msg));
    }

    public ErrorEvent(Type type, int retry) {
        this.type = type;
        this.retry = retry;
    }

    public ErrorEvent(Type type, int retry, String msg) {
        this.msg = msg;
        this.type = type;
        this.retry = retry;
    }

    public Type getType() {
        return type;
    }

    public int getRetry() {
        return retry;
    }

    public boolean isUrl() {
        return Type.URL.equals(getType());
    }

    public String getMsg() {
        if (type == Type.URL) return ResUtil.getString(R.string.error_play_url);
        if (type == Type.FLAG) return ResUtil.getString(R.string.error_play_flag);
        if (type == Type.PARSE) return ResUtil.getString(R.string.error_play_parse);
        if (type == Type.TIMEOUT) return ResUtil.getString(R.string.error_play_timeout);
        return msg;
    }

    public enum Type {
        URL, FLAG, PARSE, TIMEOUT, EXTRACT
    }
}
