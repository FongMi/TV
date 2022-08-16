package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.net.OKHttp;

import org.json.JSONObject;

import java.io.File;

public class Updater {

    private static final String URL = "https://github.com/FongMi/TV/raw/main/release/leanback.json";
    private static final String PROXY = "https://ghproxy.com/";

    public static void check() {
        new Thread(() -> new Updater().connect(URL, 0)).start();
    }

    private File getApk() {
        return FileUtil.getCacheFile("update.apk");
    }

    private void connect(String target, int retry) {
        try {
            JSONObject object = new JSONObject(OKHttp.newCall(target).execute().body().string());
            int version = object.optInt("version");
            String url = object.optString("url");
            if (retry > 0) url = PROXY + url;
            if (version <= BuildConfig.VERSION_CODE) FileUtil.clearDir(getApk());
            else FileUtil.openFile(FileUtil.write(getApk(), OKHttp.newCall(url).execute().body().bytes()));
        } catch (Exception e) {
            if (retry == 0) connect(PROXY + target, 1);
            e.printStackTrace();
        }
    }
}
