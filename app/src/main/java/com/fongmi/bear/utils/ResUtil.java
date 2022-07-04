package com.fongmi.bear.utils;

import android.util.DisplayMetrics;

import androidx.annotation.StringRes;

import com.fongmi.bear.App;

public class ResUtil {

    public static DisplayMetrics getDisplayMetrics() {
        return App.get().getResources().getDisplayMetrics();
    }

    public static int dp2px(int dpValue) {
        return Math.round(dpValue * getDisplayMetrics().density);
    }

    public static String getString(@StringRes int resId) {
        return App.get().getString(resId);
    }

    public static String getString(@StringRes int resId, Object... formatArgs) {
        return App.get().getString(resId, formatArgs);
    }
}
