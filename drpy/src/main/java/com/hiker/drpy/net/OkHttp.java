package com.hiker.drpy.net;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class OkHttp {

    private final OkHttpClient client;
    private final OkHttpClient noRedirect;

    private static class Loader {
        static volatile OkHttp INSTANCE = new OkHttp();
    }

    public static OkHttp get() {
        return Loader.INSTANCE;
    }

    public OkHttp() {
        client = getBuilder().build();
        noRedirect = client.newBuilder().followRedirects(false).followSslRedirects(false).build();
    }

    private OkHttpClient.Builder getBuilder() {
        return new OkHttpClient.Builder().sslSocketFactory(new SSLSocketFactoryCompat(), SSLSocketFactoryCompat.trustAllCert);
    }

    private OkHttpClient client() {
        return client;
    }

    private OkHttpClient noRedirect() {
        return noRedirect;
    }

    public String module(String url) {
        try {
            return client().newCall(new Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()).execute().body().string();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public Call newCall(String url, JSONObject object) {
        int redirect = object.optInt("redirect", 1);
        int timeout = object.optInt("timeout", 10000);
        OkHttpClient client = redirect == 1 ? client() : noRedirect();
        client.newBuilder().callTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).writeTimeout(timeout, TimeUnit.MILLISECONDS).connectTimeout(timeout, TimeUnit.MILLISECONDS);
        return client.newCall(getRequest(url, object));
    }

    private Request getRequest(String url, JSONObject object) {
        String method = object.optString("method", "get");
        Headers headers = getHeader(object.optJSONObject("headers"));
        if (method.equalsIgnoreCase("post")) {
            return new Request.Builder().url(url).headers(headers).post(getPostBody(object, headers.get("Content-Type"))).build();
        } else if (method.equalsIgnoreCase("header")) {
            return new Request.Builder().url(url).headers(headers).head().build();
        } else {
            return new Request.Builder().url(url).headers(headers).get().build();
        }
    }

    private Headers getHeader(JSONObject object) {
        Headers.Builder builder = new Headers.Builder();
        if (object == null) return builder.build();
        for (Iterator<String> iterator = object.keys(); iterator.hasNext(); ) {
            String key = iterator.next();
            builder.add(key, object.optString(key));
        }
        return builder.build();
    }

    private RequestBody getPostBody(JSONObject object, String contentType) {
        String body = object.optString("body").trim();
        String data = object.optString("data").trim();
        if (data.length() > 0) return RequestBody.create(data, MediaType.get("application/json"));
        if (body.length() > 0 && contentType != null) return RequestBody.create(body, MediaType.get(contentType));
        return RequestBody.create("", null);
    }
}
