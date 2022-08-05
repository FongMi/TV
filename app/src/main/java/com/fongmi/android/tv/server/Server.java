package com.fongmi.android.tv.server;

import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.utils.Utils;

import org.greenrobot.eventbus.EventBus;

public class Server implements Nano.Listener {

    private Nano nano;

    private static class Loader {
        static volatile Server INSTANCE = new Server();
    }

    public static Server get() {
        return Loader.INSTANCE;
    }

    public String getAddress(boolean local) {
        return "http://" + (local ? "127.0.0.1" : Utils.getIP()) + ":" + nano.getListeningPort() + "/";
    }

    public void start() {
        if (nano != null) return;
        try {
            nano = new Nano();
            nano.setListener(this);
            nano.start();
        } catch (Exception e) {
            nano.stop();
            nano = null;
        }
    }

    public void stop() {
        if (nano != null) {
            nano.stop();
            nano = null;
        }
    }

    @Override
    public void onSearch(String text) {
        EventBus.getDefault().post(ServerEvent.search(text));
    }

    @Override
    public void onPush(String url) {
        EventBus.getDefault().post(ServerEvent.push(url));
    }

    @Override
    public void onApi(String url) {
        EventBus.getDefault().post(ServerEvent.api(url));
    }
}
