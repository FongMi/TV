package com.github.catvod.net.interceptor;

import androidx.annotation.NonNull;

import com.github.catvod.utils.Auth;
import com.google.common.net.HttpHeaders;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RequestInterceptor implements Interceptor {

    private final ConcurrentHashMap<String, String> authMap;

    public RequestInterceptor() {
        authMap = new ConcurrentHashMap<>();
    }

    public void clear() {
        authMap.clear();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        if (request.tag(Auth.Context.class) != null || request.header(HttpHeaders.AUTHORIZATION) != null || request.url().uri().getUserInfo() != null) return chain.proceed(request);
        Request.Builder builder = request.newBuilder();
        HttpUrl url = request.url();
        checkAuth(url, builder);
        return chain.proceed(builder.build());
    }

    private void checkAuth(HttpUrl url, Request.Builder builder) {
        String host = url.host();
        String auth = url.queryParameter("auth");
        if (auth != null) authMap.put(host, auth);
        else if (authMap.containsKey(host)) builder.url(url.newBuilder().addQueryParameter("auth", authMap.get(host)).build());
    }
}
