package com.fongmi.android.tv.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.fongmi.android.tv.ui.base.ViewType;
import com.google.gson.annotations.SerializedName;

public class Style implements Parcelable {

    @SerializedName("type")
    private String type;
    @SerializedName("ratio")
    private Float ratio;

    public static Style rect() {
        return new Style("rect", 0.75f);
    }

    public static Style list() {
        return new Style("list");
    }

    public Style() {
    }

    public Style(String type) {
        this.type = type;
    }

    public Style(String type, Float ratio) {
        this.type = type;
        this.ratio = ratio;
    }

    public String getType() {
        return TextUtils.isEmpty(type) ? "rect" : type;
    }

    public Float getRatio() {
        return ratio == null || ratio <= 0 ? (isOval() ? 1.0f : 0.75f) : Math.min(4, ratio);
    }

    public boolean isRect() {
        return getType().equals("rect");
    }

    public boolean isOval() {
        return getType().equals("oval");
    }

    public boolean isList() {
        return getType().equals("list");
    }

    public boolean isLand() {
        return isRect() && getRatio() > 1.0f;
    }

    public int getViewType() {
        switch (getType()) {
            case "oval":
                return ViewType.OVAL;
            case "list":
                return ViewType.LIST;
            default:
                return ViewType.RECT;
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Style)) return false;
        Style it = (Style) obj;
        return getType().equals(it.getType()) && getRatio().equals(it.getRatio());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.type);
        dest.writeValue(this.ratio);
    }

    protected Style(Parcel in) {
        this.type = in.readString();
        this.ratio = (Float) in.readValue(Float.class.getClassLoader());
    }

    public static final Creator<Style> CREATOR = new Creator<>() {
        @Override
        public Style createFromParcel(Parcel source) {
            return new Style(source);
        }

        @Override
        public Style[] newArray(int size) {
            return new Style[size];
        }
    };
}