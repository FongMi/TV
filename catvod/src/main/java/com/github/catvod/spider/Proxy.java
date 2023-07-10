package com.github.catvod.spider;

import com.github.catvod.net.OkHttp;

public class Proxy {

    private static int port;

    static void adjustPort() throws Exception {
        if (port > 0) return;
        int port = 9978;
        while (port < 10000) {
            String resp = OkHttp.newCall("http://127.0.0.1:" + port + "/proxy?do=port").execute().body().string();
            if (resp.equals("ok")) {
                Proxy.port = port;
                break;
            }
            port++;
        }
    }

    public static String getUrl() {
        try {
            adjustPort();
            return "http://127.0.0.1:" + port + "/proxy";
        } catch (Exception e) {
            return "http://127.0.0.1:9978/proxy";
        }
    }
}
