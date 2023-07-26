package com.fongmi.quickjs.utils;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.github.catvod.Init;

public class Prefers {

    private static SharedPreferences getPrefers() {
        return PreferenceManager.getDefaultSharedPreferences(Init.getContext());
    }

    public static String get(String key, String defaultValue) {
        return getPrefers().getString(key, defaultValue);
    }

    public static String get(String key) {
        return get(key, "");
    }

    public static void put(String key, String value) {
        getPrefers().edit().putString(key, value).apply();
    }

    public static void remove(String key) {
        getPrefers().edit().remove(key).apply();
    }
}
