package com.fongmi.bear.utils;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.fongmi.bear.App;

public class Prefers {

    private static SharedPreferences getPrefers() {
        return PreferenceManager.getDefaultSharedPreferences(App.get());
    }

    public static String getString(String key) {
        return getString(key, "");
    }

    public static String getString(String key, String defaultValue) {
        return getPrefers().getString(key, defaultValue);
    }

    public static void put(String key, Object obj) {
        if (obj == null) return;
        if (obj instanceof String) {
            getPrefers().edit().putString(key, (String) obj).apply();
        } else if (obj instanceof Boolean) {
            getPrefers().edit().putBoolean(key, (Boolean) obj).apply();
        } else if (obj instanceof Float) {
            getPrefers().edit().putFloat(key, (Float) obj).apply();
        } else if (obj instanceof Integer) {
            getPrefers().edit().putInt(key, (Integer) obj).apply();
        } else if (obj instanceof Long) {
            getPrefers().edit().putLong(key, (Long) obj).apply();
        }
    }
}
