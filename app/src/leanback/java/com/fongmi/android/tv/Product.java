package com.fongmi.android.tv;

import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;

public class Product {

    public static int getDeviceType() {
        return 0;
    }

    public static int getColumn() {
        return Math.abs(Prefers.getSize() - 7);
    }

    public static void bootLive() {
        LiveActivity.start(App.activity());
    }

    public static int getEms() {
        return Math.min(ResUtil.getScreenWidth() / ResUtil.sp2px(24), 35);
    }
}
