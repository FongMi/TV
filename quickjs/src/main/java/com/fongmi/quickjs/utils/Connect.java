package com.fongmi.quickjs.utils;

import com.fongmi.quickjs.QuickJS;
import com.github.catvod.net.Net;
import com.github.catvod.net.NetOptions;
import com.github.catvod.net.NetSocket;
import com.google.gson.JsonObject;

public class Connect {

    public static Object event(NetSocket.Event event) {
        JsonObject object = new JsonObject();
        object.addProperty("type", event.type);
        if (event.type.equals("message")) object.addProperty("binary", event.binary);
        if (event.type.equals("open") || event.type.equals("close") || event.type.equals("retry")) object.addProperty("code", event.code);
        if (event.reason != null) object.addProperty("reason", event.reason);
        if (event.error != null) object.addProperty("error", event.error);
        if (event.type.equals("message") && !event.binary) return new QuickJS.Text(object.toString(), "content", (String) event.content);
        return event.binary ? new QuickJS.Binary(object.toString(), "content", (byte[]) event.content) : new QuickJS.Json(object.toString());
    }

    public static Object result(NetOptions options, Net.Result result) {
        try {
            Object[] parts = result.parts(options);
            String json = (String) parts[0];
            return result.error == null ? content(options, json, parts[1]) : new QuickJS.Text(json, "content", "");
        } catch (Exception e) {
            return error(e);
        }
    }

    private static Object content(NetOptions options, String json, Object body) {
        return switch (options.getBuffer()) {
            case 0, 2 -> new QuickJS.Text(json, "content", (String) body);
            case 1 -> new QuickJS.ByteArray(json, "content", (byte[]) body);
            case 3 -> new QuickJS.Binary(json, "content", (byte[]) body);
            default -> throw new IllegalArgumentException("Buffer must be an integer from 0 to 3");
        };
    }

    public static Object error(Throwable error) {
        return result(new NetOptions(), Net.Result.failure(error));
    }
}
