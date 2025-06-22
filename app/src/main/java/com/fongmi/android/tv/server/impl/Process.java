package com.fongmi.android.tv.server.impl;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public interface Process {

    boolean isRequest(NanoHTTPD.IHTTPSession session, String url);

    NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String url, Map<String, String> files);
}
