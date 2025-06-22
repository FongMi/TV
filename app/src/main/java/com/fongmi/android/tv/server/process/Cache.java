package com.fongmi.android.tv.server.process;

import android.text.TextUtils;

import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.server.impl.Process;
import com.github.catvod.utils.Prefers;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class Cache implements Process {

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String url) {
        return url.startsWith("/cache");
    }

    private String getKey(String rule, String key) {
        return "cache_" + (TextUtils.isEmpty(rule) ? "" : rule + "_") + key;
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String url, Map<String, String> files) {
        Map<String, String> params = session.getParms();
        String action = params.get("do");
        String rule = params.get("rule");
        String key = params.get("key");
        if ("get".equals(action)) return Nano.ok(Prefers.getString(getKey(rule, key)));
        if ("set".equals(action)) Prefers.put(getKey(rule, key), params.get("value"));
        if ("del".equals(action)) Prefers.remove(getKey(rule, key));
        return Nano.ok();
    }
}
