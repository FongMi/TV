package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.server.Nano;
import com.github.catvod.utils.Prefers;

import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD;

public class Cache implements Process {

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String path) {
        return path.equals("/cache");
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String path, Map<String, String> files) {
        Map<String, String> params = session.getParms();
        switch (Objects.requireNonNullElse(params.get("do"), "")) {
            case "get":
                return Nano.success(Prefers.getString("cache_" + params.get("key")));
            case "set":
                Prefers.put("cache_" + params.get("key"), params.get("value"));
                return Nano.success();
            case "delete":
                Prefers.remove("cache_" + params.get("key"));
                return Nano.success();
            default:
                return Nano.error(null);
        }
    }
}
