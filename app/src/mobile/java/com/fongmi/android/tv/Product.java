package com.fongmi.android.tv;

import android.content.Context;

import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;

public class Product {

    public static int getDeviceType() {
        return 1;
    }

    public static int getColumn() {
        return Math.abs(Prefers.getSize() - 5);
    }

    public static void bootLive() {
    }

    public static int[] getSpec(Context context) {
        return getSpec(context, ResUtil.dp2px(32) + ResUtil.dp2px(16 * (getColumn() - 1)), getColumn());
    }

    public static int[] getSpec(Context context, int space, int column) {
        int base = ResUtil.getScreenWidth(context) - space;
        int width = base / column;
        int height = (int) (width / 0.75f);
        return new int[]{width, height};
    }
}
