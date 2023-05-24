package com.fongmi.android.tv.player.source;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.bean.Core;
import com.google.gson.JsonObject;
import com.tvbus.engine.Listener;
import com.tvbus.engine.TVCore;

public class TVBus implements Listener {

    private TVCore tvcore;
    private String hls;

    private static class Loader {
        static volatile TVBus INSTANCE = new TVBus();
    }

    public static TVBus get() {
        return Loader.INSTANCE;
    }

    private void init(Core core) {
        tvcore = new TVCore();
        tvcore.listener(this);
        tvcore.auth(core.getAuth());
        tvcore.name(core.getName());
        tvcore.pass(core.getPass());
        tvcore.broker(core.getBroker());
        tvcore.serv(0);
        tvcore.play(8902);
        tvcore.mode(1);
        tvcore.init(App.get());
    }

    public String fetch(String url) throws InterruptedException {
        if (tvcore == null) init(LiveConfig.get().getHome().getCore());
        if (tvcore == null) return "";
        tvcore.start(url);
        onWait();
        return hls;
    }

    private void onWait() throws InterruptedException {
        synchronized (this) {
            wait();
        }
    }

    private void onNotify() {
        synchronized (this) {
            notify();
        }
    }

    public void stop() {
        if (tvcore == null) return;
        tvcore.stop();
    }

    public void quit() {
        if (tvcore == null) return;
        tvcore.quit();
        tvcore = null;
    }

    @Override
    public void onPrepared(String result) {
        JsonObject json = App.gson().fromJson(result, JsonObject.class);
        if (json.get("hls") == null) return;
        hls = json.get("hls").getAsString();
        onNotify();
    }

    @Override
    public void onStop(String result) {
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
