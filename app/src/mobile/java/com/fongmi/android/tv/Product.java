package com.fongmi.android.tv;

import android.app.Activity;

import com.fongmi.android.tv.utils.ResUtil;

import me.jessyan.autosize.AutoSizeConfig;

public class Product {

    public static int getColumn(Activity activity) {
        return ResUtil.isPort(activity) ? 3 : 6;
    }

    public static void bootLive() {
    }

    public static int getSizeInDp(boolean land) {
        return land ? AutoSizeConfig.getInstance().getDesignHeightInDp() : AutoSizeConfig.getInstance().getDesignWidthInDp();
    }
}
