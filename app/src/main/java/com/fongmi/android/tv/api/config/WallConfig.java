package com.fongmi.android.tv.api.config;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.utils.Path;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class WallConfig extends BaseConfig {

    private static final String TAG = WallConfig.class.getSimpleName();

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
        get().config(config).load(callback);
    }

    public WallConfig init() {
        return config(Config.wall());
    }

    public WallConfig config(Config config) {
        this.config = config;
        if (config.isEmpty()) return this;
        this.sync = config.getUrl().equals(VodConfig.get().getWall());
        return this;
    }

    public void load() {
        if (sync) return;
        load(new Callback());
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected Config defaultConfig() {
        return Config.wall();
    }

    @Override
    protected void postEvent() {
        super.postEvent();
        ConfigEvent.wall();
    }

    @Override
    protected void load(Config config) throws Throwable {
        File file = Path.wall(0);
        checkUrl(config.getUrl(), file);
        setWallType(file);
        setSnapshot(file);
    }

    @Override
    protected boolean isLoaded() {
        return false;
    }

    private void checkUrl(String url, File file) throws Throwable {
        if (url.startsWith("file")) FileUtil.copyAtomically(Path.local(url), file);
        else Download.create(UrlUtil.convert(url), file).tag(TAG).get();
        if (!Path.exists(file)) throw new FileNotFoundException();
    }

    private void setWallType(File file) {
        Setting.putWallType(0);
        if (isGif(file)) Setting.putWallType(1);
        else if (isVideo(file)) Setting.putWallType(2);
    }

    private void setSnapshot(File file) throws Throwable {
        Bitmap bitmap = Glide.with(App.get()).asBitmap().frame(0).load(file).override(ResUtil.getScreenWidth(), ResUtil.getScreenHeight()).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE).submit().get();
        try (FileOutputStream fos = new FileOutputStream(Path.wallCache())) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } finally {
            bitmap.recycle();
        }
    }

    private boolean isVideo(File file) {
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(file.getAbsolutePath());
            return "yes".equalsIgnoreCase(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isGif(File file) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            return "image/gif".equals(options.outMimeType);
        } catch (Exception e) {
            return false;
        }
    }

    private static class Loader {
        static volatile WallConfig INSTANCE = new WallConfig();
    }
}
