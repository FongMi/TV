package com.fongmi.quickjs.utils;

public class Proxy {

    private static int port = 9978;

    public static void set(int port) {
        Proxy.port = port;
    }

    public static String getUrl() {
        return "http://127.0.0.1:" + port + "/proxy";
    }
}
