package com.fongmi.android.tv.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class Collect implements Parcelable {

    private boolean activated;
    private List<Vod> list;
    private Site site;
    private int page;

    public static Collect all() {
        Collect item = new Collect(Site.get("all", ResUtil.getString(R.string.all)), new ArrayList<>());
        item.setActivated(true);
        return item;
    }

    public static Collect create(List<Vod> list) {
        return new Collect(list.get(0).getSite(), list);
    }

    public Collect() {
    }

    public Collect(Site site, List<Vod> list) {
        this.site = site;
        this.list = list;
    }

    public Site getSite() {
        return site == null ? new Site() : site;
    }

    public List<Vod> getList() {
        return list == null ? new ArrayList<>() : list;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public int getPage() {
        return Math.max(1, page);
    }

    public void setPage(int page) {
        this.page = page;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.activated ? (byte) 1 : (byte) 0);
        dest.writeTypedList(this.list);
        dest.writeParcelable(this.site, flags);
        dest.writeInt(this.page);
    }

    protected Collect(Parcel in) {
        this.activated = in.readByte() != 0;
        this.list = in.createTypedArrayList(Vod.CREATOR);
        this.site = in.readParcelable(Site.class.getClassLoader());
        this.page = in.readInt();
    }

    public static final Creator<Collect> CREATOR = new Creator<>() {
        @Override
        public Collect createFromParcel(Parcel source) {
            return new Collect(source);
        }

        @Override
        public Collect[] newArray(int size) {
            return new Collect[size];
        }
    };
}
