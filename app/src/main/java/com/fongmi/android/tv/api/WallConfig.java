package com.fongmi.android.tv.api;

import android.graphics.drawable.Drawable;

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
    private String url;

    private static class Loader {
        static volatile WallConfig INSTANCE = new WallConfig();
    }

    public static WallConfig get() {
        return Loader.INSTANCE;
    }

    public static String getUrl() {
        return get().url;
    }

    public static Drawable drawable(Drawable drawable) {
        if (get().drawable != null) return drawable;
        get().setDrawable(drawable);
        return drawable;
    }

    public WallConfig init() {
        this.url = Config.wall().getUrl();
        return this;
    }

    public WallConfig config(Config config) {
        setUrl(config.getUrl());
        return this;
    }

    public WallConfig clear() {
        this.url = null;
        return this;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }

    public void load() {
        load(new Callback());
    }

    public void load(Callback callback) {
        new Thread(() -> loadConfig(callback)).start();
    }

    private void loadConfig(Callback callback) {
        try {
            File file = write(FileUtil.getWall(0));
            if (file.exists() && file.length() > 0) refresh(0);
            else setUrl(ApiConfig.get().getWall());
            App.post(callback::success);
        } catch (Exception e) {
            App.post(() -> callback.error(R.string.error_config_parse));
            setUrl(ApiConfig.get().getWall());
            e.printStackTrace();
        }
    }

    private File write(File file) throws IOException {
        if (url.startsWith("file")) FileUtil.copy(FileUtil.getLocal(url), file);
        else if (url.startsWith("http")) FileUtil.write(file, Product.resize(OkHttp.newCall(url).execute().body().bytes()));
        else file.delete();
        return file;
    }

    public static void refresh(int index) {
        Prefers.putWall(index);
        RefreshEvent.wall();
    }
}
