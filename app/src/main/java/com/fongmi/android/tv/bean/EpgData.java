package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.utils.Formatters;
import com.github.catvod.utils.Trans;
import com.google.gson.annotations.SerializedName;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class EpgData {

    @SerializedName("title")
    private String title;
    @SerializedName("start")
    private String start;
    @SerializedName("end")
    private String end;

    private boolean selected;
    private long startTime;
    private long endTime;

    public String getTitle() {
        return TextUtils.isEmpty(title) ? "" : title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStart() {
        return TextUtils.isEmpty(start) ? "" : start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return TextUtils.isEmpty(end) ? "" : end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setSelected(EpgData item) {
        this.selected = item.equals(this);
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public boolean isInRange() {
        return isInRange(System.currentTimeMillis());
    }

    boolean isInRange(long now) {
        return getStartTime() <= now && now < getEndTime();
    }

    public boolean isFuture() {
        return getStartTime() > System.currentTimeMillis();
    }

    public String format() {
        if (getTitle().isEmpty()) return "";
        if (getStart().isEmpty() && getEnd().isEmpty()) return getTitle();
        return getTime() + "  " + getTitle();
    }

    public String getTime() {
        if (getStart().isEmpty() && getEnd().isEmpty()) return "";
        return formatTime(getStart()) + " ~ " + formatTime(getEnd());
    }

    private static String formatTime(String time) {
        if (time.isEmpty()) return time;
        if (time.equals("24:00:00")) return "24:00";
        try {
            return LocalTime.parse(time).format(Formatters.TIME);
        } catch (DateTimeParseException e) {
            return time;
        }
    }

    public String getRange() {
        return "clock=" + Formatters.EPG_RANGE.format(Instant.ofEpochMilli(getStartTime())) + "-" + Formatters.EPG_RANGE.format(Instant.ofEpochMilli(getEndTime()));
    }

    public void checkDay(ZoneId zoneId) {
        setEndTime(Instant.ofEpochMilli(getEndTime()).atZone(zoneId).plusDays(1).toInstant().toEpochMilli());
    }

    public void trans() {
        if (Trans.pass()) return;
        this.title = Trans.s2t(title);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EpgData it)) return false;
        return Objects.equals(getTitle(), it.getTitle()) && Objects.equals(getEnd(), it.getEnd()) && Objects.equals(getStart(), it.getStart());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), getEnd(), getStart());
    }
}
