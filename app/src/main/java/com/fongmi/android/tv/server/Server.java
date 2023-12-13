package com.fongmi.android.tv.server;

import com.fongmi.android.tv.App;
import com.github.catvod.Proxy;
import com.github.catvod.utils.Util;

import go_proxy_video.GoVideoProxy;
import go_proxy_video.Go_proxy_video;

public class Server {

    private GoVideoProxy proxy;
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

    public int getPort() {
        return port;
    }

    public String getAddress() {
        return getAddress(false);
    }

    public String getAddress(String path) {
        return getAddress(true) + "/" + path;
    }

    public String getAddress(boolean local) {
        return "http://" + (local ? "127.0.0.1" : Util.getIp()) + ":" + getPort();
    }

    public void go() {
        if (proxy != null) proxy.stop();
        proxy = Go_proxy_video.newGoVideoProxy();
        App.execute(() -> proxy.start());
    }

    public void start() {
        if (nano != null) return;
        do {
            try {
                nano = new Nano(port);
                Proxy.set(port);
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
        if (proxy != null) {
            proxy.stop();
            proxy = null;
        }
    }
}
