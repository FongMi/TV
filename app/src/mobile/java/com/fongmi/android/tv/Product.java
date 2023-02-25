package com.fongmi.android.tv;

import android.content.res.Resources;

import com.fongmi.android.tv.utils.Prefers;

public class Product {

    public static Resources hackResources(Resources resources) {
        return resources;
    }

    public static int getColumn() {
        return Math.abs(Prefers.getSize() - 5);
    }

    public static void bootLive() {
    }
}
