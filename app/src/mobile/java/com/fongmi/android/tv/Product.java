package com.fongmi.android.tv;

import android.content.Context;

import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.utils.ResUtil;

public class Product {

    public static int getDeviceType() {
        return 1;
    }

    public static int getColumn() {
        return Math.abs(Setting.getSize() - 5);
    }

    public static void bootLive() {
    }

    public static int[] getSpec(Context context) {
        return getSpec(context, ViewType.GRID);
    }

    public static int[] getSpec(Context context, int viewType) {
        return getSpec(context, ResUtil.dp2px(32) + ResUtil.dp2px(16 * (getColumn() - 1)), getColumn(), viewType);
    }

    public static int[] getSpec(Context context, int space, int column) {
        return getSpec(context, space, column, ViewType.GRID);
    }

    public static int[] getSpec(Context context, int space, int column, int viewType) {
        int base = ResUtil.getScreenWidth(context) - space;
        int width = base / column;
        return new int[]{width, getHeight(viewType, width)};
    }

    private static int getHeight(int viewType, int value) {
        if (viewType == ViewType.GRID) return (int) (value / 0.75f);
        if (viewType == ViewType.LAND) return (int) (value * 0.75f);
        return value;
    }
}
