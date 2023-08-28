package com.fongmi.android.tv.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Cate implements Parcelable {

    @SerializedName("land")
    private int land;

    public Cate() {
    }

    public int getLand() {
        return land;
    }

    public void setLand(int land) {
        this.land = land;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.land);
    }

    protected Cate(Parcel in) {
        this.land = in.readInt();
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
