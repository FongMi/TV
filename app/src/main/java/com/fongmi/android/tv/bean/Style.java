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
    private float ratio;

    public static Style rect() {
        return new Style("rect", 0.75f);
    }

    public static Style list() {
        return new Style("list");
    }

    public static Style get(int land, int circle, float ratio) {
        if (land == 1) return new Style("rect", ratio == 0 ? 1.33f : ratio);
        if (circle == 1) return new Style("oval", ratio == 0 ? 1.0f : ratio);
        return null;
    }

    public Style() {
    }

    public Style(String type) {
        this.type = type;
    }

    public Style(String type, float ratio) {
        this.type = type;
        this.ratio = ratio;
    }

    public String getType() {
        return TextUtils.isEmpty(type) ? "rect" : type;
    }

    public float getRatio() {
        return ratio <= 0 ? (isOval() ? 1.0f : 0.75f) : Math.min(4, ratio);
    }

    public boolean isRect() {
        return "rect".equals(getType());
    }

    public boolean isOval() {
        return "oval".equals(getType());
    }

    public boolean isList() {
        return "list".equals(getType());
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
        return getType().equals(it.getType()) && getRatio() == it.getRatio();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.type);
        dest.writeFloat(this.ratio);
    }

    protected Style(Parcel in) {
        this.type = in.readString();
        this.ratio = in.readFloat();
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
