package com.fongmi.android.tv;

import android.content.res.Resources;

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
        return Math.abs(getSize() - 7);
    }
}
