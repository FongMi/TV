package com.fongmi.quickjs.utils;

import com.fongmi.quickjs.bean.Req;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Util;
import com.google.gson.Gson;
import com.whl.quickjs.wrapper.JSArray;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JSUtil {

    public static JSArray toArray(QuickJSContext ctx, List<String> items) {
        JSArray array = ctx.createNewJSArray();
        if (items == null || items.isEmpty()) return array;
        for (int i = 0; i < items.size(); i++) array.set(items.get(i), i);
        return array;
    }

    public static JSArray toArray(QuickJSContext ctx, byte[] bytes) {
        JSArray array = ctx.createNewJSArray();
        if (bytes == null || bytes.length == 0) return array;
        for (int i = 0; i < bytes.length; i++) array.set(bytes[i], i);
        return array;
    }

    public static JSObject toObj(QuickJSContext ctx, Map<String, String> map) {
        JSObject obj = ctx.createNewJSObject();
        if (map == null || map.isEmpty()) return obj;
        for (String s : map.keySet()) obj.setProperty(s, map.get(s));
        return obj;
    }

    public static Call call(String url, Req req) {
        OkHttpClient client = req.getRedirect() == 1 ? OkHttp.client() : OkHttp.noRedirect();
        client = client.newBuilder().connectTimeout(req.getTimeout(), TimeUnit.MILLISECONDS).readTimeout(req.getTimeout(), TimeUnit.MILLISECONDS).writeTimeout(req.getTimeout(), TimeUnit.MILLISECONDS).build();
        return client.newCall(getRequest(url, req, Headers.of(req.getHeader())));
    }

    public static JSObject toResponse(QuickJSContext ctx, Response response, Req req) throws IOException {
        JSObject jsObject = ctx.createNewJSObject();
        JSObject jsHeader = ctx.createNewJSObject();
        setHeader(ctx, response, jsHeader);
        jsObject.setProperty("headers", jsHeader);
        if (req.getBuffer() == 0) jsObject.setProperty("content", new String(response.body().bytes(), req.getCharset()));
        if (req.getBuffer() == 1) jsObject.setProperty("content", JSUtil.toArray(ctx, response.body().bytes()));
        if (req.getBuffer() == 2) jsObject.setProperty("content", Util.base64(response.body().bytes()));
        return jsObject;
    }

    public static JSObject toFailure(QuickJSContext ctx) {
        JSObject jsObject = ctx.createNewJSObject();
        JSObject jsHeader = ctx.createNewJSObject();
        jsObject.setProperty("headers", jsHeader);
        jsObject.setProperty("content", "");
        return jsObject;
    }

    private static Request getRequest(String url, Req req, Headers headers) {
        if (req.getMethod().equalsIgnoreCase("post")) {
            return new Request.Builder().url(url).headers(headers).post(getPostBody(req, headers.get("Content-Type"))).build();
        } else if (req.getMethod().equalsIgnoreCase("header")) {
            return new Request.Builder().url(url).headers(headers).head().build();
        } else {
            return new Request.Builder().url(url).headers(headers).get().build();
        }
    }

    private static RequestBody getPostBody(Req req, String contentType) {
        if (req.getData() != null && req.getPostType().equals("form")) return getFormBody(req);
        if (req.getData() != null) return RequestBody.create(new Gson().toJson(req.getData()), MediaType.get("application/json"));
        if (req.getBody() != null && contentType != null) return RequestBody.create(req.getBody(), MediaType.get(contentType));
        return RequestBody.create("", null);
    }

    private static RequestBody getFormBody(Req req) {
        FormBody.Builder formBody = new FormBody.Builder();
        Map<String, String> params = Json.toMap(req.getData());
        for (String key : params.keySet()) formBody.add(key, params.get(key));
        return formBody.build();
    }

    private static void setHeader(QuickJSContext ctx, Response response, JSObject object) {
        for (Map.Entry<String, List<String>> entry : response.headers().toMultimap().entrySet()) {
            if (entry.getValue().size() == 1) object.setProperty(entry.getKey(), entry.getValue().get(0));
            if (entry.getValue().size() >= 2) object.setProperty(entry.getKey(), JSUtil.toArray(ctx, entry.getValue()));
        }
    }
}
