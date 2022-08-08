package com.fongmi.android.tv.server;

import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.utils.Utils;

import org.greenrobot.eventbus.EventBus;

public class Server implements Nano.Listener {

    private Nano nano;
    private int port;

    private static class Loader {
        static volatile Server INSTANCE = new Server();
    }

    public static Server get() {
        return Loader.INSTANCE;
    }

    public Server() {
        this.port = 9978;
    }

    public String getAddress(boolean local) {
        return "http://" + (local ? "127.0.0.1" : Utils.getIP()) + ":" + port;
    }

    public void start() {
        if (nano != null) return;
        do {
            try {
                nano = new Nano(port);
                nano.setListener(this);
                nano.start();
                break;
            } catch (Exception e) {
                ++port;
                nano.stop();
                nano = null;
            }
        } while (port < 9999);
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
