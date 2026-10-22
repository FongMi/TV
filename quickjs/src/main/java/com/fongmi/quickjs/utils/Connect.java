package com.fongmi.quickjs.utils;

import com.fongmi.quickjs.QuickJS;
import com.github.catvod.net.Net;
import com.github.catvod.net.NetOptions;
import com.github.catvod.net.NetSocket;
import com.github.catvod.utils.Util;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.charset.Charset;

public class Connect {

    private static final Gson GSON = new Gson();

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
            JsonObject object = new JsonObject();
            if (result.code instanceof Number code) object.addProperty("code", code.intValue());
            else object.addProperty("code", String.valueOf(result.code));
            object.add("headers", GSON.toJsonTree(result.headers));
            if (result.error != null) {
                object.addProperty("content", "");
                object.addProperty("error", result.error);
            } else {
                return content(options, result, object);
            }
            return new QuickJS.Json(object.toString());
        } catch (Exception e) {
            return error(e);
        }
    }

    private static Object content(NetOptions options, Net.Result result, JsonObject object) {
        switch (options.getBuffer()) {
            case 0 -> {
                return new QuickJS.Text(object.toString(), "content", new String(result.body, Charset.forName(options.getCharset())));
            }
            case 1 -> {
                return new QuickJS.ByteArray(object.toString(), "content", result.body);
            }
            case 2 -> object.addProperty("content", Util.base64(result.body));
            case 3 -> {
                return new QuickJS.Binary(object.toString(), "content", result.body);
            }
        }
        return new QuickJS.Json(object.toString());
    }

    public static Object error(Throwable error) {
        return result(new NetOptions(), Net.Result.failure(error));
    }
}
