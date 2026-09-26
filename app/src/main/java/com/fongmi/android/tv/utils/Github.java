package com.fongmi.android.tv.utils;

import org.json.JSONArray;
import org.json.JSONObject;

public class Github {

    private static final String REPO = "FongMi/Release";

    public static String getRelease() {
        return "https://api.github.com/repos/" + REPO + "/releases/latest";
    }

    public static String getApk(JSONObject release, String name) {
        JSONArray assets = release.optJSONArray("assets");
        if (assets == null) return "";
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.optJSONObject(i);
            if (asset != null && name.equals(asset.optString("name")) && "uploaded".equals(asset.optString("state"))) return asset.optString("browser_download_url");
        }
        return "";
    }
}
