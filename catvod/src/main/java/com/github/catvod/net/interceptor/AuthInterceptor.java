package com.github.catvod.net.interceptor;

import androidx.annotation.NonNull;

import com.github.catvod.utils.Auth;
import com.google.common.net.HttpHeaders;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final ConcurrentHashMap<String, String> userMap;

    public AuthInterceptor() {
        userMap = new ConcurrentHashMap<>();
    }

    public void clear() {
        userMap.clear();
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (request.header(HttpHeaders.AUTHORIZATION) != null) return chain.proceed(request);
        request = check(request);
        Response response = chain.proceed(request);
        if (response.code() != 401) return response;
        Request challenged = response.request();
        String user = getCredentials(request, challenged);
        if (user == null) return response;
        response.close();
        String header = response.header(HttpHeaders.WWW_AUTHENTICATE);
        String auth = digest(header) ? Auth.digest(user, header, challenged) : Auth.basic(user);
        return chain.proceed(challenged.newBuilder().header(HttpHeaders.AUTHORIZATION, auth).build());
    }

    private String getCredentials(Request request, Request challenged) {
        String user = challenged.url().uri().getUserInfo();
        if (user == null && request.url().scheme().equals(challenged.url().scheme()) && request.url().host().equals(challenged.url().host()) && request.url().port() == challenged.url().port()) user = request.url().uri().getUserInfo();
        return user == null && request.tag(Auth.Context.class) == null ? userMap.get(challenged.url().host()) : user;
    }

    private boolean digest(String header) {
        return header != null && header.startsWith("Digest");
    }

    private Request check(Request request) {
        URI uri = request.url().uri();
        if (uri.getUserInfo() == null) return request;
        if (request.tag(Auth.Context.class) == null) userMap.put(request.url().host(), uri.getUserInfo());
        return request.newBuilder().header(HttpHeaders.AUTHORIZATION, Auth.basic(uri.getUserInfo())).build();
    }
}
