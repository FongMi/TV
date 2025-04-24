package com.github.catvod;

import com.github.catvod.utils.Util;

public class Proxy {

    private static int port = -1;

    public static void set(int port) {
        Proxy.port = port;
    }

    public static int getPort() {
        return port;
    }

    public static String getUrl(boolean local) {
        return "http://" + (local ? "127.0.0.1" : Util.getIp()) + ":" + getPort() + "/proxy";
    }
}
