package com.fongmi.android.tv;

import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.utils.ResUtil;

public class Product {

    public static int getDeviceType() {
        return 0;
    }

    public static int getColumn() {
        return Math.abs(Setting.getSize() - 7);
    }

    public static int getColumn(int viewType) {
        if (viewType == ViewType.LAND) return getColumn() - 1;
        if (viewType == ViewType.FULL) return 3;
        return getColumn();
    }

    public static void bootLive() {
        LiveActivity.start(App.activity());
    }

    public static int[] getSpec(int viewType) {
        int column = getColumn(viewType);
        int space = ResUtil.dp2px(48) + ResUtil.dp2px(16 * (column - 1));
        return getSpec(space, column, viewType);
    }

    private static int[] getSpec(int space, int column, int viewType) {
        int base = ResUtil.getScreenWidth() - space;
        int width = base / column;
        return new int[]{width, getHeight(viewType, width)};
    }

    private static int getHeight(int viewType, int value) {
        if (viewType == ViewType.LAND) return (int) (value * 0.75f);
        if (viewType == ViewType.OVAL) return value;
        return (int) (value / 0.75f);
    }

    public static int getEms() {
        return Math.min(ResUtil.getScreenWidth() / ResUtil.sp2px(24), 35);
    }
}
