package com.fongmi.android.tv.api.config;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;

import java.io.File;
import java.io.FileOutputStream;

public class WallConfig {

    private Config config;
    private boolean sync;

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

    public static void load(Config config, Callback callback) {
        get().clear().config(config).load(callback);
    }

    public WallConfig init() {
        return config(Config.wall());
    }

    public WallConfig config(Config config) {
        this.config = config;
        if (config.getUrl() == null) return this;
        this.sync = config.getUrl().equals(VodConfig.get().getWall());
        return this;
    }

    public WallConfig clear() {
        this.config = null;
        return this;
    }

    public Config getConfig() {
        return config == null ? Config.wall() : config;
    }

    public void load(Callback callback) {
        App.execute(() -> loadConfig(callback));
    }

    private void loadConfig(Callback callback) {
        try {
            File file = write(FileUtil.getWall(0));
            if (file.exists() && file.length() > 0) refresh(0);
            else config(Config.find(VodConfig.get().getWall(), 2));
            App.post(callback::success);
            config.update();
        } catch (Throwable e) {
            App.post(() -> callback.error(Notify.getError(R.string.error_config_parse, e)));
            config(Config.find(VodConfig.get().getWall(), 2));
            e.printStackTrace();
        }
    }

    private File write(File file) throws Exception {
        Path.write(file, OkHttp.newCall(UrlUtil.convert(getUrl())).execute().body().bytes());
        return resize(file);
    }

    private File resize(File file) {
        try {
            Bitmap bitmap = Glide.with(App.get()).asBitmap().load(file).centerCrop().override(ResUtil.getScreenWidth(), ResUtil.getScreenHeight()).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).submit().get();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));
            bitmap.recycle();
            return file;
        } catch (Exception e) {
            return file;
        }
    }

    public boolean needSync(String url) {
        return sync || TextUtils.isEmpty(config.getUrl()) || url.equals(config.getUrl());
    }

    public static void refresh(int index) {
        Setting.putWall(index);
        RefreshEvent.wall();
    }
}
