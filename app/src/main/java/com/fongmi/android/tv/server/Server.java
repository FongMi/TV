package com.fongmi.android.tv.server;

import com.github.catvod.Proxy;
import com.github.catvod.utils.Util;

import go_proxy_video.Go_proxy_video;

public class Server {
    private Nano nano;
    private int port;

    private Thread goThread;

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
        if (goThread == null) {
            goThread = new Thread(Go_proxy_video::start);
            goThread.start();
        }
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
        if (goThread != null) {
            goThread.interrupt();
            goThread = null;
        }
    }
}
