package com.fongmi.android.tv.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.github.catvod.utils.Trans;
import com.google.gson.annotations.SerializedName;

public class Value implements Parcelable {

    @SerializedName("n")
    private String n;
    @SerializedName("v")
    private String v;

    private boolean activated;

    public Value() {
    }

    public Value(String v) {
        this.v = v;
    }

    public Value(String n, String v) {
        this.n = Trans.s2t(n);
        this.v = v;
    }

    public String getN() {
        return TextUtils.isEmpty(n) ? "" : n;
    }

    public String getV() {
        return TextUtils.isEmpty(v) ? "" : v;
    }

    public void setV(String v) {
        this.v = v;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public void setActivated(Value item) {
        this.activated = item.equals(this);
    }

    public void trans() {
        this.n = Trans.s2t(n);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Value)) return false;
        Value it = (Value) obj;
        return getV().equals(it.getV());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.n);
        dest.writeString(this.v);
        dest.writeByte(this.activated ? (byte) 1 : (byte) 0);
    }

    protected Value(Parcel in) {
        this.n = in.readString();
        this.v = in.readString();
        this.activated = in.readByte() != 0;
    }

    public static final Creator<Value> CREATOR = new Creator<>() {
        @Override
        public Value createFromParcel(Parcel source) {
            return new Value(source);
        }

        @Override
        public Value[] newArray(int size) {
            return new Value[size];
        }
    };
}
