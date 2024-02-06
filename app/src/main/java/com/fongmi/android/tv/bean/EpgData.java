package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Locale;

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

    public String getEnd() {
        return TextUtils.isEmpty(end) ? "" : end;
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
        return getStartTime() <= System.currentTimeMillis() && System.currentTimeMillis() <= getEndTime();
    }

    public boolean isFuture() {
        return getStartTime() > System.currentTimeMillis();
    }

    public String format(String group) {
        String pattern = group.split("\\)")[1].split("\\}")[0];
        if (group.contains("(b)")) return new SimpleDateFormat(pattern, Locale.getDefault()).format(getStartTime());
        if (group.contains("(e)")) return new SimpleDateFormat(pattern, Locale.getDefault()).format(getEndTime());
        return "";
    }

    public String format() {
        if (getTitle().isEmpty()) return "";
        if (getStart().isEmpty() && getEnd().isEmpty()) return ResUtil.getString(R.string.play_now, getTitle());
        return getStart() + " ~ " + getEnd() + "  " + getTitle();
    }

    public String getTime() {
        if (getStart().isEmpty() && getEnd().isEmpty()) return "";
        return getStart() + " ~ " + getEnd();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EpgData)) return false;
        EpgData it = (EpgData) obj;
        return getTitle().equals(it.getTitle()) && getEnd().equals(it.getEnd()) && getStart().equals(it.getStart());
    }

    @Override
    public int hashCode() {
        int result = getTitle().hashCode();
        result = 31 * result + getEnd().hashCode();
        result = 31 * result + getStart().hashCode();
        return result;
    }
}
