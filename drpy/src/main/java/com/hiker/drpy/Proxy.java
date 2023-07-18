package com.hiker.drpy;

import com.github.catvod.net.OkHttp;

public class Proxy {

    private static int port;

    static void tryPort() {
        if (port > 0) return;
        int port = 9978;
        while (port < 9999) {
            boolean ok = string().equals("ok");
            if (ok) Proxy.port = port;
            if (ok) break;
            port++;
        }
    }

    private static String string() {
        try {
            return OkHttp.newCall("http://127.0.0.1:" + port + "/proxy?do=port").execute().body().string();
        } catch (Exception e) {
            return "";
        }
    }

    public static String getUrl() {
        tryPort();
        return "http://127.0.0.1:" + port + "/proxy";
    }
}
