package com.fongmi.android.tv.bean;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.utils.ResUtil;

public class Func {

    private final int resId;
    private final int id;
    private int drawable;
    private int nextFocusLeft;
    private int nextFocusRight;

    public static Func create(int resId) {
        return new Func(resId);
    }

    public Func(int resId) {
        this.resId = resId;
        this.id = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 ? View.generateViewId() : -1;
        this.setDrawable();
    }

    public int getResId() {
        return resId;
    }

    public int getId() {
        return id;
    }

    public int getDrawable() {
        return drawable;
    }

    public int getNextFocusLeft() {
        return nextFocusLeft;
    }

    public void setNextFocusLeft(int nextFocusLeft) {
        this.nextFocusLeft = nextFocusLeft;
    }

    public int getNextFocusRight() {
        return nextFocusRight;
    }

    public void setNextFocusRight(int nextFocusRight) {
        this.nextFocusRight = nextFocusRight;
    }

    public String getText() {
        return ResUtil.getString(resId);
    }

    @SuppressLint("NonConstantResourceId")
    public void setDrawable() {
        switch (resId) {
            case R.string.home_history_short:
                this.drawable = R.drawable.ic_home_history;
                break;
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
