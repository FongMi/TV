package com.fongmi.android.tv.player.source;

import android.os.SystemClock;
import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Core;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tvbus.engine.TVCore;
import com.tvbus.engine.TVListener;
import com.tvbus.engine.TVService;

public class TVBus implements TVListener {

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

    public void init(Core core) {
        tvcore = TVCore.getInstance().listener(this);
        TVService.start(App.get(), core.getAuth(), core.getName(), core.getPass());
    }

    public String fetch(String url) {
        tvcore.start(url);
        while (TextUtils.isEmpty(hls)) SystemClock.sleep(50);
        String temp = hls;
        hls = null;
        return temp;
    }

    public void stop() {
        TVService.stop(App.get());
    }

    @Override
    public void onPrepared(String result) {
        JsonObject json = gson.fromJson(result, JsonObject.class);
        if (json.get("hls") == null) return;
        hls = json.get("hls").getAsString();
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
