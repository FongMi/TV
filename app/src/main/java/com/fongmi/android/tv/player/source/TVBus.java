package com.fongmi.android.tv.player.source;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.bean.Core;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tvbus.engine.Listener;
import com.tvbus.engine.TVCore;

public class TVBus implements Listener {

    private final Gson gson;
    private TVCore tvcore;
    private String hls;

    private static class Loader {
        static volatile TVBus INSTANCE = new TVBus();
    }

    public static TVBus get() {
        return Loader.INSTANCE;
    }

    public TVBus() {
        this.gson = new Gson();
    }

    private void init(Core core) {
        if (core == null) return;
        tvcore = new TVCore().listener(this);
        tvcore.auth(core.getAuth()).name(core.getName()).pass(core.getPass()).broker(core.getBroker());
        tvcore.serv(0).play(8902).mode(1).init(App.get());
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
        JsonObject json = gson.fromJson(result, JsonObject.class);
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
