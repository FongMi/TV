package com.fongmi.android.tv.utils;

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.AnimRes;
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

    public static int getScreenHeightPx() {
        return getDisplayMetrics().heightPixels;
    }

    public static boolean isLand() {
        return App.get().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static boolean isPort() {
        return App.get().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public static int getEms() {
        return Math.min(getScreenWidthPx() / sp2px(24), 35);
    }

    public static int sp2px(int sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getDisplayMetrics());
    }

    public static int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getDisplayMetrics());
    }

    public static int getDrawable(String resId) {
        return App.get().getResources().getIdentifier(resId, "drawable", App.get().getPackageName());
    }

    public static String getString(@StringRes int resId) {
        return App.get().getString(resId);
    }

    public static String getString(@StringRes int resId, Object... formatArgs) {
        return App.get().getString(resId, formatArgs);
    }

    public static String[] getStringArray(@ArrayRes int resId) {
        return App.get().getResources().getStringArray(resId);
    }

    public static Drawable getDrawable(@DrawableRes int resId) {
        return ContextCompat.getDrawable(App.get(), resId);
    }

    public static Animation getAnim(@AnimRes int resId) {
        return AnimationUtils.loadAnimation(App.get(), resId);
    }
}
