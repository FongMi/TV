package com.fongmi.android.tv.player.extractor;

import android.net.Uri;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Core;
import com.fongmi.android.tv.exception.ExtractException;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.google.gson.JsonObject;
import com.tvbus.engine.Listener;
import com.tvbus.engine.TVCore;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class TVBus implements Source.Extractor, Listener {

    private CountDownLatch latch;
    private volatile String hls;
    private TVCore tvcore;
    private Core core;

    @Override
    public boolean match(String scheme, String host) {
        return "tvbus".equals(scheme);
    }

    private void init(Core core) {
        try {
            App.get().setHook(core.getHook());
            tvcore = new TVCore(getPath(core.getSo())).listener(this).auth(core.getAuth()).name(core.getName()).pass(core.getPass()).domain(core.getDomain()).broker(core.getBroker()).serv(0).play(8902).mode(1).init();
        } catch (Exception ignored) {
        } finally {
            App.get().setHook(null);
        }
    }

    private String getPath(String url) throws Exception {
        File file = new File(Path.so(), Uri.parse(url).getLastPathSegment());
        if (file.length() < 10240) Path.write(file, OkHttp.newCall(url).execute().body().bytes());
        return file.getAbsolutePath();
    }

    @Override
    public String fetch(String url) throws Exception {
        Core c = LiveConfig.get().getHome().getCore();
        if (core != null && !core.equals(c)) change();
        if (tvcore == null) init(core = c);
        latch = new CountDownLatch(1);
        tvcore.start(url);
        latch.await();
        return check();
    }

    private String check() throws Exception {
        if (hls.startsWith("-")) throw new ExtractException(ResUtil.getString(R.string.error_play_code, hls));
        return hls;
    }

    private void change() {
        Setting.putBootLive(true);
        App.post(() -> System.exit(0), 250);
    }

    @Override
    public void stop() {
        if (tvcore != null) tvcore.stop();
        hls = null;
    }

    @Override
    public void exit() {
        if (tvcore != null) tvcore.quit();
        tvcore = null;
    }

    @Override
    public void onPrepared(String result) {
        JsonObject json = App.gson().fromJson(result, JsonObject.class);
        if (json.get("hls") == null) return;
        hls = json.get("hls").getAsString();
        latch.countDown();
    }

    @Override
    public void onStop(String result) {
        JsonObject json = App.gson().fromJson(result, JsonObject.class);
        hls = json.get("errno").getAsString();
        if (hls.startsWith("-")) latch.countDown();
    }

    @Override
    public void onInited(String result) {
    }

    @Override
    public void onStart(String result) {
    }

    @Override
    public void onInfo(String result) {
    }

    @Override
    public void onQuit(String result) {
    }
}
