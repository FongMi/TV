package com.fongmi.android.tv;

import android.app.Activity;
import android.content.res.Resources;

import com.fongmi.android.tv.utils.ResUtil;

public class Product {

    public static Resources hackResources(Resources resources) {
        return resources;
    }

    public static int getColumn(Activity activity) {
        return ResUtil.isPort(activity) ? 3 : 6;
    }
}
