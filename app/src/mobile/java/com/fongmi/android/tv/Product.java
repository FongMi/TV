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

    public static int getColumn(int viewType) {
        return viewType == ViewType.LAND ? getColumn() - 1 : getColumn();
    }

    public static void bootLive() {
    }

    public static int[] getSpec(Context context) {
        return getSpec(context, ViewType.GRID);
    }

    public static int[] getSpec(Context context, int viewType) {
        int column = getColumn(viewType);
        int space = ResUtil.dp2px(32) + ResUtil.dp2px(16 * (column - 1));
        if (viewType == ViewType.OVAL) space += ResUtil.dp2px(column * 16);
        return getSpec(context, space, column, viewType);
    }

    public static int[] getSpec(Context context, int space, int column) {
        return getSpec(context, space, column, ViewType.GRID);
    }

    private static int[] getSpec(Context context, int space, int column, int viewType) {
        int base = ResUtil.getScreenWidth(context) - space;
        int width = base / column;
        return new int[]{width, getHeight(viewType, width)};
    }

    private static int getHeight(int viewType, int value) {
        if (viewType == ViewType.LAND) return (int) (value * 0.75f);
        if (viewType == ViewType.OVAL) return value;
        return (int) (value / 0.75f);
    }
}
