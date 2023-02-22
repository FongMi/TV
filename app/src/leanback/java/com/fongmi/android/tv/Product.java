package com.fongmi.android.tv;

import android.content.res.Resources;

import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.utils.Prefers;

import me.jessyan.autosize.AutoSizeCompat;

public class Product {

    public static int getColumn() {
        return Math.abs(Prefers.getSize() - 7);
    }

    public static void bootLive() {
        LiveActivity.start(App.activity());
    }

    public static int getSizeInDp(boolean land) {
        return land ? AutoSizeConfig.getInstance().getDesignWidthInDp() : AutoSizeConfig.getInstance().getDesignHeightInDp();
    }
}
