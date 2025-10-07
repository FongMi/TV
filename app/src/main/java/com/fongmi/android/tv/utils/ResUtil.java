package com.fongmi.android.tv.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.AnimRes;
import androidx.annotation.ArrayRes;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;

public class ResUtil {

    public static DisplayMetrics getDisplayMetrics() {
        return getDisplayMetrics(App.get());
    }

    public static DisplayMetrics getDisplayMetrics(Context context) {
        return context.getResources().getDisplayMetrics();
    }

    public static int getScreenWidth() {
        return getScreenWidth(App.get());
    }

    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            return getDisplayMetrics(context).widthPixels;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Rect rect = windowManager.getCurrentWindowMetrics().getBounds();
            return isLand(context) ? Math.max(rect.width(), rect.height()) : Math.min(rect.width(), rect.height());
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.widthPixels;
        }
    }

    public static int getScreenHeight() {
        return getScreenHeight(App.get());
    }

    public static int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            return getDisplayMetrics(context).heightPixels;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Rect rect = windowManager.getCurrentWindowMetrics().getBounds();
            return isLand(context) ? Math.min(rect.width(), rect.height()) : Math.max(rect.width(), rect.height());
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            return displayMetrics.heightPixels;
        }
    }

    public static boolean isEdge(Context context, MotionEvent e, int edge) {
        return e.getRawX() < edge || e.getRawX() > getScreenWidth(context) - edge || e.getRawY() < edge || e.getRawY() > getScreenHeight(context) - edge;
    }

    public static boolean isLand(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public static boolean isPad() {
        return App.get().getResources().getConfiguration().smallestScreenWidthDp >= 600;
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
        return App.get().getResources().getString(resId);
    }

    public static String getString(@StringRes int resId, Object... formatArgs) {
        return App.get().getResources().getString(resId, formatArgs);
    }

    public static String[] getStringArray(@ArrayRes int resId) {
        return App.get().getResources().getStringArray(resId);
    }

    public static TypedArray getTypedArray(@ArrayRes int resId) {
        return App.get().getResources().obtainTypedArray(resId);
    }

    public static Drawable getDrawable(@DrawableRes int resId) {
        return ContextCompat.getDrawable(App.get(), resId);
    }

    public static int getColor(@ColorRes int resId) {
        return ContextCompat.getColor(App.get(), resId);
    }

    public static Animation getAnim(@AnimRes int resId) {
        return AnimationUtils.loadAnimation(App.get(), resId);
    }

    public static Display getDisplay(Context context) {
        return ContextCompat.getDisplayOrDefault(context);
    }

    public static int getTextWidth(String content, int size) {
        Paint paint = new Paint();
        paint.setTextSize(sp2px(size));
        return (int) paint.measureText(content);
    }
}
