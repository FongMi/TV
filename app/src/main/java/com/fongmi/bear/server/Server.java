package com.fongmi.bear.server;

import fi.iki.elonen.NanoHTTPD;

public class Server extends NanoHTTPD {

    public static int port = 9978;

    public Server() {
        super(port);
    }
}
