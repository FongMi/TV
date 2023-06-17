package com.fongmi.android.tv.event;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.EventBus;

public class ErrorEvent {

    private final Type type;
    private final boolean retry;

    public static void url() {
        EventBus.getDefault().post(new ErrorEvent(Type.URL, false));
    }

    public static void parse() {
        EventBus.getDefault().post(new ErrorEvent(Type.PARSE, false));
    }

    public static void format(boolean retry) {
        EventBus.getDefault().post(new ErrorEvent(Type.FORMAT, retry));
    }

    public static void episode() {
        EventBus.getDefault().post(new ErrorEvent(Type.EPISODE, false));
    }

    public static void timeout() {
        EventBus.getDefault().post(new ErrorEvent(Type.TIMEOUT, false));
    }

    public ErrorEvent(Type type, boolean retry) {
        this.type = type;
        this.retry = retry;
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

    public boolean isRetry() {
        return retry;
    }

    public boolean isFormat() {
        return Type.FORMAT.equals(getType());
    }

    public String getMsg() {
        return getResId() == -1 ? "" : ResUtil.getString(getResId());
    }

    public enum Type {
        URL, PARSE, FORMAT, EPISODE, TIMEOUT
    }
}
