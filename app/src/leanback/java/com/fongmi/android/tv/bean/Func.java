package com.fongmi.android.tv.bean;

import android.annotation.SuppressLint;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

public class Func {

    private final int resId;
    private int drawable;

    public static Func create(int resId) {
        return new Func(resId);
    }

    public Func(int resId) {
        this.resId = resId;
        this.setDrawable();
    }

    public int getResId() {
        return resId;
    }

    public int getDrawable() {
        return drawable;
    }

    public String getText() {
        return ResUtil.getString(resId);
    }

    @SuppressLint("NonConstantResourceId")
    public void setDrawable() {
        switch (resId) {
            case R.string.home_vod:
                this.drawable = R.drawable.ic_home_vod;
                break;
            case R.string.home_live:
                this.drawable = R.drawable.ic_home_live;
                break;
            case R.string.home_keep:
                this.drawable = R.drawable.ic_home_keep;
                break;
            case R.string.home_push:
                this.drawable = R.drawable.ic_home_push;
                break;
            case R.string.home_search:
                this.drawable = R.drawable.ic_home_search;
                break;
            case R.string.home_setting:
                this.drawable = R.drawable.ic_home_setting;
                break;
        }
    }
}
