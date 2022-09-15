package com.fongmi.android.tv.utils;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.fongmi.android.tv.App;

public class Prefers {

    private static SharedPreferences getPrefers() {
        return PreferenceManager.getDefaultSharedPreferences(App.get());
    }

    public static String getString(String key, String defaultValue) {
        return getPrefers().getString(key, defaultValue);
    }

    public static String getString(String key) {
        return getString(key, "");
    }

    public static int getInt(String key, int defaultValue) {
        return getPrefers().getInt(key, defaultValue);
    }

    public static int getInt(String key) {
        return getInt(key, 0);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return getPrefers().getBoolean(key, defaultValue);
    }

    public static boolean getBoolean(String key) {
        return getPrefers().getBoolean(key, false);
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

    public static String getUrl() {
        return getString("url");
    }

    public static void putUrl(String url) {
        put("url", url);
    }

    public static String getHome() {
        return getString("home");
    }

    public static void putHome(String home) {
        put("home", home);
    }

    public static int getRender() {
        return getInt("render", 0);
    }

    public static void putRender(int render) {
        put("render", render);
    }

    public static int getQuality() {
        return getInt("quality", 1);
    }

    public static void putQuality(int quality) {
        put("quality", quality);
    }

    public static int getSize() {
        return getInt("size", 2);
    }

    public static void putSize(int size) {
        put("size", size);
    }

    public static boolean isFFmpeg() {
        return getBoolean("ffmpeg");
    }

    public static void putFFmpeg(boolean ffmpeg) {
        put("ffmpeg", ffmpeg);
    }

    public static String getParse() {
        return getString("parse");
    }

    public static void putParse(String parse) {
        put("parse", parse);
    }

    public static String getKeyword() {
        return getString("keyword");
    }

    public static void putKeyword(String keyword) {
        put("keyword", keyword);
    }

    public static int getScale() {
        return getInt("scale");
    }

    public static void putScale(int scale) {
        put("scale", scale);
    }

    public static int getInterval() {
        return getInt("interval", 15);
    }

    public static void putInterval(int interval) {
        put("interval", interval);
    }

    public static boolean getUpdate() {
        return getBoolean("update", true);
    }

    public static void putUpdate(boolean update) {
        put("update", update);
    }

    public static float getThumbnail() {
        return 0.3f * getQuality() + 0.4f;
    }

    public static int getColumn() {
        return Math.abs(getSize() - 7);
    }
}
