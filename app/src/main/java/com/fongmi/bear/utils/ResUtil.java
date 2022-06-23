package com.fongmi.bear.utils;

import androidx.annotation.StringRes;

import com.fongmi.bear.App;

public class ResUtil {

    public static String getString(@StringRes int resId) {
        return App.get().getString(resId);
    }
}
