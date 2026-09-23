package com.fongmi.android.tv.bean;

import com.google.gson.annotations.SerializedName;

/** A playerContent skip range in milliseconds; only an ending range may omit end. */
public class SkipSegment {

    @SerializedName("type")
    private String type;
    @SerializedName("start")
    private Long start;
    @SerializedName("end")
    private Long end;

    public boolean isOpening() {
        return "opening".equals(type);
    }

    public boolean isMiddle() {
        return "middle".equals(type);
    }

    public boolean isEnding() {
        return "ending".equals(type);
    }

    public long getStart() {
        return start == null ? -1 : start;
    }

    public long getEnd() {
        return end == null ? -1 : end;
    }

    public boolean isToEnd() {
        return end == null;
    }

    public boolean isValid() {
        if (getStart() < 0 || (!isOpening() && !isMiddle() && !isEnding())) return false;
        return isToEnd() ? isEnding() && getStart() > 0 : getEnd() > getStart();
    }

    public boolean contains(long position) {
        return isValid() && position >= getStart() && (isToEnd() || position < getEnd());
    }
}
