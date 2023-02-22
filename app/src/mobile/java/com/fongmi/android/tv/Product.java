package com.fongmi.android.tv;

import android.app.Activity;
import android.content.res.Resources;

import com.fongmi.android.tv.utils.ResUtil;

public class Product {

    public static Resources hackResources(Resources resources) {
        try {
            //AutoSizeCompat.autoConvertDensityOfGlobal(resources);
            return resources;
        } catch (Exception ignored) {
            return resources;
        }
    }

    public static int getColumn(Activity activity) {
        return ResUtil.isPort(activity) ? 3 : 6;
    }

    public static void bootLive() {
    }
}
