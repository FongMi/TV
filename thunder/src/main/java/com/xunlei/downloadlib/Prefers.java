package com.xunlei.downloadlib;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefers {

    private static final String FILE_NAME = "thunder_data";

    public static void put(Context context, String key, Object object) {
        put(context, FILE_NAME, key, object);
    }

    public static void put(Context context, String fileName, String key, Object object) {
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        if (object instanceof String) {
            sp.edit().putString(key, (String) object).apply();
        } else if (object instanceof Integer) {
            sp.edit().putInt(key, (Integer) object).apply();
        } else if (object instanceof Boolean) {
            sp.edit().putBoolean(key, (Boolean) object).apply();
        } else if (object instanceof Float) {
            sp.edit().putFloat(key, (Float) object).apply();
        } else if (object instanceof Long) {
            sp.edit().putLong(key, (Long) object).apply();
        } else {
            sp.edit().putString(key, object.toString()).apply();
        }
    }

    public static Object get(Context context, String key, Object defaultObject) {
        return get(context, FILE_NAME, key, defaultObject);
    }

    public static Object get(Context context, String fileName, String key, Object defaultObject) {
        SharedPreferences sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        if (defaultObject instanceof String) {
            return sp.getString(key, (String) defaultObject);
        } else if (defaultObject instanceof Integer) {
            return sp.getInt(key, (Integer) defaultObject);
        } else if (defaultObject instanceof Boolean) {
            return sp.getBoolean(key, (Boolean) defaultObject);
        } else if (defaultObject instanceof Float) {
            return sp.getFloat(key, (Float) defaultObject);
        } else if (defaultObject instanceof Long) {
            return sp.getLong(key, (Long) defaultObject);
        } else {
            return null;
        }
    }

    public static String getString(Context context, String key, String defaultObject) {
        return getString(context, FILE_NAME, key, defaultObject);
    }

    public static String getString(Context context, String fileName, String key, String defaultObject) {
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE).getString(key, defaultObject);
    }

    public static int getInt(Context context, String key, int defaultObject) {
        return getInt(context, FILE_NAME, key, defaultObject);
    }

    public static int getInt(Context context, String fileName, String key, int defaultObject) {
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE).getInt(key, defaultObject);
    }

    public static boolean getBoolean(Context context, String key, boolean defaultObject) {
        return getBoolean(context, FILE_NAME, key, defaultObject);
    }

    public static boolean getBoolean(Context context, String fileName, String key, boolean defaultObject) {
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE).getBoolean(key, defaultObject);
    }

    public static float getFloat(Context context, String key, float defaultObject) {
        return getFloat(context, FILE_NAME, key, defaultObject);
    }

    public static float getFloat(Context context, String fileName, String key, float defaultObject) {
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE).getFloat(key, defaultObject);
    }

    public static long getLong(Context context, String key, long defaultObject) {
        return getLong(context, FILE_NAME, key, defaultObject);
    }

    public static long getLong(Context context, String fileName, String key, long defaultObject) {
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE).getLong(key, defaultObject);
    }
}