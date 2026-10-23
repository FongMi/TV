package com.fongmi.android.tv.bean;

import com.google.gson.annotations.SerializedName;

import java.util.regex.Pattern;

import okhttp3.HttpUrl;

public class Timeshift {

    private static final Pattern TIME = Pattern.compile("\\{time\\}|%7Btime%7D", Pattern.CASE_INSENSITIVE);

    @SerializedName("url")
    private String url;
    @SerializedName("start")
    private long start;
    @SerializedName("window")
    private long window;

    public boolean isValid(long now) {
        if (url == null || !TIME.matcher(url).find() || start <= 0 || start >= now || window <= 0) return false;
        return HttpUrl.parse(getUrl(now)) != null;
    }

    public long getStart(long now) {
        return Math.max(start, now - window);
    }

    public String getUrl(long time) {
        return TIME.matcher(url).replaceAll(String.valueOf(time));
    }
}
