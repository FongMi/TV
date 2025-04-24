package com.fongmi.android.tv.server;

import com.fongmi.android.tv.player.Players;
import com.github.catvod.Proxy;
import com.github.catvod.utils.Util;

public class Server {

    private Players player;
    private Nano nano;
    private int port;

    private static class Loader {
        static volatile Server INSTANCE = new Server();
    }

    public static Server get() {
        return Loader.INSTANCE;
    }

    public Server() {
        this.port = 8964;
    }

    public int getPort() {
        return port;
    }

    public Players getPlayer() {
        return player;
    }

    public void setPlayer(Players player) {
        this.player = player;
    }

    public String getAddress() {
        return getAddress(false);
    }

    public String getAddress(int tab) {
        return getAddress(false) + "?tab=" + tab;
    }

    public String getAddress(String path) {
        return getAddress(true) + path;
    }

    public String getAddress(boolean local) {
        return "http://" + (local ? "127.0.0.1" : Util.getIp()) + ":" + getPort();
    }

    public void start() {
        if (nano != null) return;
        while (port < 9999) {
            try {
                nano = new Nano(port);
                Proxy.set(port);
                nano.start();
                return;
            } catch (Exception e) {
                nano = null;
                port++;
            }
        }
    }

    public void stop() {
        if (nano != null) nano.stop();
        nano = null;
    }
}
