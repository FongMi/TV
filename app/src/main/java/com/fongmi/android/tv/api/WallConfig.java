package com.fongmi.android.tv.api;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Prefers;

import java.io.File;
import java.io.IOException;

public class WallConfig {

    private Drawable drawable;
    private Config config;
    private boolean same;

    private static class Loader {
        static volatile WallConfig INSTANCE = new WallConfig();
    }

    public static WallConfig get() {
        return Loader.INSTANCE;
    }

    public static String getUrl() {
        return get().getConfig().getUrl();
    }

    public static String getDesc() {
        return get().getConfig().getDesc();
    }

    public static Drawable drawable(Drawable drawable) {
        if (get().drawable != null) return drawable;
        get().setDrawable(drawable);
        return drawable;
    }

    public WallConfig init() {
        this.config = Config.wall();
        return this;
    }

    public WallConfig config(Config config) {
        this.config = config;
        this.same = config.getUrl().equals(ApiConfig.get().getWall());
        return this;
    }

    public WallConfig clear() {
        this.config = null;
        return this;
    }

    public Config getConfig() {
        return config == null ? Config.wall() : config;
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }

    public void load(Callback callback) {
        new Thread(() -> loadConfig(callback)).start();
    }

    private void loadConfig(Callback callback) {
        try {
            File file = write(FileUtil.getWall(0));
            if (file.exists() && file.length() > 0) refresh(0);
            else config(Config.find(ApiConfig.get().getWall(), 2));
            App.post(callback::success);
            config.update();
        } catch (Exception e) {
            App.post(() -> callback.error(R.string.error_config_parse));
            config(Config.find(ApiConfig.get().getWall(), 2));
            e.printStackTrace();
        }
    }

    private File write(File file) throws IOException {
        if (getUrl().startsWith("file")) FileUtil.copy(FileUtil.getLocal(getUrl()), file);
        else if (getUrl().startsWith("http")) FileUtil.write(file, Product.resize(OkHttp.newCall(getUrl()).execute().body().bytes()));
        else file.delete();
        return file;
    }

    public boolean isSame(String url) {
        return same || TextUtils.isEmpty(config.getUrl()) || url.equals(config.getUrl());
    }

    public static void refresh(int index) {
        Prefers.putWall(index);
        RefreshEvent.wall();
    }
}
