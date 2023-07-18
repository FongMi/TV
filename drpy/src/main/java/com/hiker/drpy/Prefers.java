package com.hiker.drpy;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.lang.ref.WeakReference;

public class Prefers {

    private WeakReference<Context> context;

    private static class Loader {
        static volatile Prefers INSTANCE = new Prefers();
    }

    private static Prefers get() {
        return Loader.INSTANCE;
    }

    private Context getContext() {
        return context.get();
    }

    public static void setContext(Context context) {
        get().context = new WeakReference<>(context);
    }

    private static SharedPreferences getPrefers() {
        return PreferenceManager.getDefaultSharedPreferences(get().getContext());
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
