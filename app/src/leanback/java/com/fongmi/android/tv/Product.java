package com.fongmi.android.tv;

import android.content.res.Resources;

import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.utils.Prefers;

import me.jessyan.autosize.AutoSizeCompat;

public class Product {

    public static Resources hackResources(Resources resources) {
        try {
            AutoSizeCompat.autoConvertDensityOfGlobal(resources);
            return resources;
        } catch (Exception ignored) {
            return resources;
        }
    }

    public static int getColumn() {
        return Math.abs(Prefers.getSize() - 7);
    }

    public static void bootLive() {
        LiveActivity.start(App.activity());
    }
}
