package com.fongmi.android.tv.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Cate implements Parcelable {

    @SerializedName("land")
    private int land;

    @SerializedName("circle")
    private int circle;

    @SerializedName("ratio")
    private float ratio;

    public Cate() {
    }

    public int getLand() {
        return land;
    }

    public int getCircle() {
        return circle;
    }

    public float getRatio() {
        return ratio;
    }

    public Style getStyle() {
        return Style.get(getLand(), getCircle(), getRatio());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.land);
        dest.writeInt(this.circle);
        dest.writeFloat(this.ratio);
    }

    protected Cate(Parcel in) {
        this.land = in.readInt();
        this.circle = in.readInt();
        this.ratio = in.readFloat();
    }

    public static final Creator<Cate> CREATOR = new Creator<>() {
        @Override
        public Cate createFromParcel(Parcel source) {
            return new Cate(source);
        }

        @Override
        public Cate[] newArray(int size) {
            return new Cate[size];
        }
    };
}
