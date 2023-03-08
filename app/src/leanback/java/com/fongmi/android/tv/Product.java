package com.fongmi.android.tv;

import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.utils.Prefers;

public class Product {

    public static int getColumn() {
        return Math.abs(Prefers.getSize() - 7);
    }

    public static void bootLive() {
        LiveActivity.start(App.activity());
    }
}
