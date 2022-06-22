package com.fongmi.bear.utils;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.fongmi.bear.App;

public class Prefers {

    private static SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(App.get());
    }

    private static String getString(String key, String defaultValue) {
        return getPreferences().getString(key, defaultValue);
    }

    private static void putString(String key, String value) {
        getPreferences().edit().putString(key, value).apply();
    }

    private static int getInt(String key, int defaultValue) {
        return getPreferences().getInt(key, defaultValue);
    }

    private static void putInt(String key, int value) {
        getPreferences().edit().putInt(key, value).apply();
    }

    private static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    private static boolean getBoolean(String key, boolean defaultValue) {
        return getPreferences().getBoolean(key, defaultValue);
    }

    private static void putBoolean(String key, boolean value) {
        getPreferences().edit().putBoolean(key, value).apply();
    }
}
