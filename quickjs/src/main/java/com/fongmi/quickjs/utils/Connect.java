package com.fongmi.quickjs.utils;

import com.fongmi.quickjs.bean.Req;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Util;
import com.google.common.net.HttpHeaders;
import com.whl.quickjs.wrapper.JSObject;
import com.whl.quickjs.wrapper.QuickJSContext;

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

public class Connect {

    public static Call to(String url, Req req) {
        OkHttpClient client = req.getRedirect() == 1 ? OkHttp.client() : OkHttp.noRedirect();
        client = client.newBuilder().connectTimeout(req.getTimeout(), TimeUnit.MILLISECONDS).readTimeout(req.getTimeout(), TimeUnit.MILLISECONDS).writeTimeout(req.getTimeout(), TimeUnit.MILLISECONDS).build();
        return client.newCall(getRequest(url, req, Headers.of(req.getHeader())));
    }

    public static JSObject success(QuickJSContext ctx, Req req, Response res) {
        try {
            JSObject jsObject = ctx.createNewJSObject();
            JSObject jsHeader = ctx.createNewJSObject();
            setHeader(ctx, res, jsHeader);
            jsObject.setProperty("headers", jsHeader);
            if (req.getBuffer() == 0) jsObject.setProperty("content", new String(res.body().bytes(), req.getCharset()));
            if (req.getBuffer() == 1) jsObject.setProperty("content", JSUtil.toArray(ctx, res.body().bytes()));
            if (req.getBuffer() == 2) jsObject.setProperty("content", Util.base64(res.body().bytes()));
            return jsObject;
        } catch (Exception e) {
            return error(ctx);
        }
    }

    public static JSObject error(QuickJSContext ctx) {
        JSObject jsObject = ctx.createNewJSObject();
        JSObject jsHeader = ctx.createNewJSObject();
        jsObject.setProperty("headers", jsHeader);
        jsObject.setProperty("content", "");
        return jsObject;
    }

    private static Request getRequest(String url, Req req, Headers headers) {
        if (req.getMethod().equalsIgnoreCase("post")) {
            return new Request.Builder().url(url).headers(headers).post(getPostBody(req, headers.get(HttpHeaders.CONTENT_TYPE))).build();
        } else if (req.getMethod().equalsIgnoreCase("header")) {
            return new Request.Builder().url(url).headers(headers).head().build();
        } else {
            return new Request.Builder().url(url).headers(headers).get().build();
        }
    }

    private static RequestBody getPostBody(Req req, String contentType) {
        if (req.getData() != null && req.getPostType().equals("json")) return getJsonBody(req);
        if (req.getData() != null && req.getPostType().equals("form")) return getFormBody(req);
        if (req.getBody() != null && contentType != null) return RequestBody.create(req.getBody(), MediaType.get(contentType));
        return RequestBody.create("", null);
    }

    private static RequestBody getJsonBody(Req req) {
        return RequestBody.create(req.getData().toString(), MediaType.get("application/json"));
    }

    private static RequestBody getFormBody(Req req) {
        FormBody.Builder formBody = new FormBody.Builder();
        Map<String, String> params = Json.toMap(req.getData());
        for (String key : params.keySet()) formBody.add(key, params.get(key));
        return formBody.build();
    }

    private static void setHeader(QuickJSContext ctx, Response res, JSObject object) {
        for (Map.Entry<String, List<String>> entry : res.headers().toMultimap().entrySet()) {
            if (entry.getValue().size() == 1) object.setProperty(entry.getKey(), entry.getValue().get(0));
            if (entry.getValue().size() >= 2) object.setProperty(entry.getKey(), JSUtil.toArray(ctx, entry.getValue()));
        }
    }
}
