package com.fongmi.android.tv.event;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.EventBus;

public class ErrorEvent {

    private final Type type;
    private final int retry;
    private String msg;

    public static void url() {
        EventBus.getDefault().post(new ErrorEvent(Type.URL, 0));
    }

    public static void parse() {
        EventBus.getDefault().post(new ErrorEvent(Type.PARSE, 0));
    }

    public static void format(int retry) {
        EventBus.getDefault().post(new ErrorEvent(Type.FORMAT, retry));
    }

    public static void episode() {
        EventBus.getDefault().post(new ErrorEvent(Type.EPISODE, 0));
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
        this.type = type;
        this.retry = retry;
        this.msg = msg;
    }

    private int getResId() {
        if (type == Type.URL) return R.string.error_play_url;
        if (type == Type.PARSE) return R.string.error_play_parse;
        if (type == Type.FORMAT) return R.string.error_play_format;
        if (type == Type.EPISODE) return R.string.error_play_episode;
        if (type == Type.TIMEOUT) return R.string.error_play_timeout;
        return -1;
    }

    public Type getType() {
        return type;
    }

    public int getRetry() {
        return retry;
    }

    public boolean isFormat() {
        return Type.FORMAT.equals(getType());
    }

    public String getMsg() {
        return getResId() == -1 ? msg : ResUtil.getString(getResId());
    }

    public enum Type {
        URL, PARSE, FORMAT, EPISODE, TIMEOUT, EXTRACT
    }
}
