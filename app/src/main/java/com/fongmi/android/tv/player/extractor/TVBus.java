package com.fongmi.android.tv.player.extractor;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.bean.Core;
import com.fongmi.android.tv.player.Source;
import com.google.gson.JsonObject;
import com.tvbus.engine.Listener;
import com.tvbus.engine.TVCore;

public class TVBus implements Source.Extractor, Listener {

    private TVCore tvcore;
    private String hls;

    @Override
    public boolean match(String scheme, String host) {
        return scheme.equals("tvbus");
    }

    private void init(Core core) {
        tvcore = new TVCore();
        tvcore.auth(core.getAuth()).broker(core.getBroker());
        tvcore.name(core.getName()).pass(core.getPass());
        tvcore.serv(0).play(8902).mode(1).listener(this);
        tvcore.init(App.get());
    }

    @Override
    public String fetch(String url) throws Exception {
        if (tvcore == null) init(LiveConfig.get().getHome().getCore());
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

    @Override
    public void stop() {
        if (tvcore != null) tvcore.stop();
        if (hls != null) hls = null;
    }

    @Override
    public void exit() {
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
