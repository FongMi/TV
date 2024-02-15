package com.github.catvod.utils;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.github.catvod.Init;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.Map;

public class Prefers {

    private static SharedPreferences getPrefers() {
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
        } else if (obj instanceof LazilyParsedNumber) {
            getPrefers().edit().putInt(key, ((LazilyParsedNumber) obj).intValue()).apply();
        }
    }

    public static void remove(String key) {
        getPrefers().edit().remove(key).apply();
    }

    public static void backup(File file) {
        Path.write(file, new Gson().toJson(getPrefers().getAll()).getBytes());
    }

    public static void restore(File file) {
        try {
            Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LAZILY_PARSED_NUMBER).create();
            Map<String, Object> map = gson.fromJson(Path.read(file), new TypeToken<Map<String, Object>>() {}.getType());
            for (Map.Entry<String, ?> entry : map.entrySet()) Prefers.put(entry.getKey(), convert(entry));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Path.clear(file);
        }
    }

    private static Object convert(Map.Entry<String, ?> entry) {
        if (entry.getKey().equals("danmu_size")) {
            return Float.parseFloat(entry.getValue().toString());
        } else {
            return entry.getValue();
        }
    }
}
