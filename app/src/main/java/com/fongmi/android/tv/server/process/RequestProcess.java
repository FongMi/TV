package com.fongmi.android.tv.server.process;

import fi.iki.elonen.NanoHTTPD;

public interface RequestProcess {

    boolean isRequest(NanoHTTPD.IHTTPSession session, String path);

    NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String path);
}
