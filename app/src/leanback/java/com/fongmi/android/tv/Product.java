package com.fongmi.android.tv;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.utils.ResUtil;

public class Product {

    public static int getDeviceType() {
        return 0;
    }

    public static int getColumn() {
        return Math.abs(Setting.getSize() - 7);
    }

    public static int getColumn(Vod.Style style) {
        if (style.isLand()) return getColumn() - 1;
        if (style.isFull()) return 3;
        return getColumn();
    }

    public static int[] getSpec(Vod.Style style) {
        int column = getColumn(style);
        int space = ResUtil.dp2px(48) + ResUtil.dp2px(16 * (column - 1));
        if (style.isOval()) space += ResUtil.dp2px(column * 16);
        return getSpec(space, column, style);
    }

    private static int[] getSpec(int space, int column, Vod.Style style) {
        int base = ResUtil.getScreenWidth() - space;
        int width = base / column;
        int height = (int) (width / style.getRatio());
        return new int[]{width, height};
    }

    public static int getEms() {
        return Math.min(ResUtil.getScreenWidth() / ResUtil.sp2px(24), 35);
    }
}
