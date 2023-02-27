package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.server.Nano;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class InputRequestProcess implements RequestProcess {

    private final Nano nano;

    public InputRequestProcess(Nano nano) {
        this.nano = nano;
    }

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String path) {
        return session.getMethod() == NanoHTTPD.Method.POST && path.equals("/action");
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String path) {
        if (!path.equals("/action")) return Nano.createPlainTextResponse(NanoHTTPD.Response.Status.NOT_FOUND, "Error 404, file not found.");
        Map<String, String> params = session.getParms();
        switch (params.get("do")) {
            case "search":
                nano.getListener().onSearch(params.get("word").trim());
                break;
            case "push":
                nano.getListener().onPush(params.get("url").trim());
                break;
            case "api":
                nano.getListener().onApi(params.get("url").trim());
                break;
        }
        return Nano.createPlainTextResponse(NanoHTTPD.Response.Status.OK, "ok");
    }
}
