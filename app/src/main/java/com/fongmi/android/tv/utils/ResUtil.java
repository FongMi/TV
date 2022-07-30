package com.fongmi.android.tv.utils;

import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;

import androidx.annotation.ArrayRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;

public class ResUtil {

    public static DisplayMetrics getDisplayMetrics() {
        return App.get().getResources().getDisplayMetrics();
    }

    public static int getScreenWidthPx() {
        return getDisplayMetrics().widthPixels;
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

    public static CharSequence[] getStringArray(@ArrayRes int resId) {
        return App.get().getResources().getStringArray(resId);
    }

    public static Drawable getDrawable(@DrawableRes int resId) {
        return ContextCompat.getDrawable(App.get(), resId);
    }
}
