package com.github.catvod.utils;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.github.catvod.Init;
import com.google.gson.internal.LazilyParsedNumber;

public class Prefers {

    public static SharedPreferences getPrefers() {
        return PreferenceManager.getDefaultSharedPreferences(Init.context());
    }

    public static String getString(String key) {
        return getString(key, "");
    }

    public static String getString(String key, String defaultValue) {
        try {
            return getPrefers().getString(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static int getInt(String key) {
        return getInt(key, 0);
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return getPrefers().getInt(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static float getFloat(String key) {
        return getFloat(key, 0f);
    }

    public static float getFloat(String key, float defaultValue) {
        try {
            return getPrefers().getFloat(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        try {
            return getPrefers().getBoolean(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
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
        } else if (obj instanceof LazilyParsedNumber number) {
            if (number.toString().contains(".")) put(key, number.floatValue());
            else put(key, number.intValue());
        }
    }

    public static void remove(String key) {
        getPrefers().edit().remove(key).apply();
    }
}
