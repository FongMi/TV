package com.fongmi.android.tv.server;

import com.fongmi.android.tv.utils.Utils;

public class Server implements Nano.Listener {

    private Nano mNano;

    private static class Loader {
        static volatile Server INSTANCE = new Server();
    }

    public static Server get() {
        return Loader.INSTANCE;
    }

    public String getAddress(boolean local) {
        return "http://" + (local ? "127.0.0.1" : Utils.getIP()) + ":" + mNano.getListeningPort() + "/";
    }

    public void start() {
        if (mNano != null) return;
        try {
            mNano = new Nano();
            mNano.setListener(this);
            mNano.start();
        } catch (Exception e) {
            mNano.stop();
            mNano = null;
        }
    }

    public void stop() {
        if (mNano != null) {
            mNano.stop();
            mNano = null;
        }
    }

    @Override
    public void onTextReceived(String text) {

    }

    @Override
    public void onApiReceived(String url) {

    }

    @Override
    public void onPushReceived(String url) {

    }
}
