package com.github.catvod.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Keep;

import com.github.catvod.Runtime;

@Keep
public final class Local {

    private final String name;

    public Local(String siteKey) {
        if (siteKey == null || siteKey.isEmpty()) throw new IllegalArgumentException("Local requires a site key");
        name = "spider_local_" + Crypto.sha256(siteKey);
    }

    public String get(String key) {
        return getPrefs().getString(key(key), null);
    }

    public void set(String key, String value) {
        Json.strict(value);
        getPrefs().edit().putString(key(key), value).apply();
    }

    public void delete(String key) {
        getPrefs().edit().remove(key(key)).apply();
    }

    public void clear() {
        getPrefs().edit().clear().apply();
    }

    private SharedPreferences getPrefs() {
        return Runtime.context().getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    private String key(String key) {
        if (key == null || key.isEmpty()) throw new IllegalArgumentException("Local requires a nonempty key");
        return key;
    }
}
