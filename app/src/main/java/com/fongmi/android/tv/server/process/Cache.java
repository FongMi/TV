package com.fongmi.android.tv.server.process;

import android.text.TextUtils;

import com.fongmi.android.tv.server.Nano;
import com.github.catvod.utils.Prefers;

import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD;

public class Cache implements Process {

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String path) {
        return "/cache".equals(path);
    }

    private String getKey(String rule, String key) {
        return "cache_" + (TextUtils.isEmpty(rule) ? "" : rule + "_") + key;
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String path, Map<String, String> files) {
        Map<String, String> params = session.getParms();
        String rule = params.get("rule");
        String key = params.get("key");
        switch (Objects.requireNonNullElse(params.get("do"), "")) {
            case "get":
                return Nano.success(Prefers.getString(getKey(rule, key)));
            case "set":
                Prefers.put(getKey(rule, key), params.get("value"));
                return Nano.success();
            case "del":
                Prefers.remove(getKey(rule, key));
                return Nano.success();
            default:
                return Nano.error(null);
        }
    }
}
