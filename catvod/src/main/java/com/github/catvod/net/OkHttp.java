package com.github.catvod.net;

import android.net.Uri;

import androidx.collection.ArrayMap;

import com.github.catvod.bean.Doh;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Prefers;
import com.github.catvod.utils.Util;
import com.google.common.net.HttpHeaders;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.Credentials;
import okhttp3.Dns;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.dnsoverhttps.DnsOverHttps;

public class OkHttp {

    private static final int TIMEOUT = 30 * 1000;
    private static final int CACHE = 100 * 1024 * 1024;

    private DnsOverHttps dns;
    private OkHttpClient client;
    private OkHttpClient clientProxy;

    private static class Loader {
        static volatile OkHttp INSTANCE = new OkHttp();
    }

    public static OkHttp get() {
        return Loader.INSTANCE;
    }

    public static Dns dns() {
        return get().dns != null ? get().dns : Dns.SYSTEM;
    }

    public static Uri proxy() {
        return Uri.parse(Prefers.getString("proxy"));
    }

    public void setDoh(Doh doh) {
        OkHttpClient dohClient = new OkHttpClient.Builder().cache(new Cache(Path.doh(), CACHE)).hostnameVerifier(SSLCompat.VERIFIER).sslSocketFactory(new SSLCompat(), SSLCompat.TM).build();
        dns = doh.getUrl().isEmpty() ? null : new DnsOverHttps.Builder().client(dohClient).url(HttpUrl.get(doh.getUrl())).bootstrapDnsHosts(doh.getHosts()).build();
        clientProxy = null;
        client = null;
    }

    public void setProxy() {
        Authenticator.setDefault(null);
        clientProxy = null;
    }

    public static OkHttpClient client() {
        if (get().client != null) return get().client;
        return get().client = getBuilder().build();
    }

    public static OkHttpClient clientProxy() {
        if (get().clientProxy != null) return get().clientProxy;
        return get().clientProxy = getBuilder(proxy()).build();
    }

    public static OkHttpClient client(int timeout) {
        return client().newBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).writeTimeout(timeout, TimeUnit.MILLISECONDS).build();
    }

    public static OkHttpClient clientProxy(int timeout) {
        return clientProxy().newBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).writeTimeout(timeout, TimeUnit.MILLISECONDS).build();
    }

    public static OkHttpClient noRedirect(int timeout) {
        return client().newBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).writeTimeout(timeout, TimeUnit.MILLISECONDS).followRedirects(false).followSslRedirects(false).build();
    }

    public static OkHttpClient noRedirectProxy(int timeout) {
        return clientProxy().newBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).writeTimeout(timeout, TimeUnit.MILLISECONDS).followRedirects(false).followSslRedirects(false).build();
    }

    public static OkHttpClient client(boolean proxy, boolean redirect, int timeout) {
        if (proxy) return redirect ? clientProxy(timeout) : noRedirectProxy(timeout);
        else return redirect ? client(timeout) : noRedirect(timeout);
    }

    public static Call newCall(String url) {
        Uri uri = Uri.parse(url);
        if (uri.getUserInfo() != null) return newCall(url, Headers.of(HttpHeaders.AUTHORIZATION, Util.basic(uri)));
        return client().newCall(new Request.Builder().url(url).build());
    }

    public static Call newCall(OkHttpClient client, String url) {
        return client.newCall(new Request.Builder().url(url).build());
    }

    public static Call newCall(String url, Headers headers) {
        return client().newCall(new Request.Builder().url(url).headers(headers).build());
    }

    public static Call newCall(String url, ArrayMap<String, String> params) {
        return client().newCall(new Request.Builder().url(buildUrl(url, params)).build());
    }

    public static Call newCall(String url, ArrayMap<String, String> params, Headers headers) {
        return client().newCall(new Request.Builder().url(buildUrl(url, params)).headers(headers).build());
    }

    public static Call newCall(String url, RequestBody body, Headers headers) {
        return client().newCall(new Request.Builder().url(url).post(body).headers(headers).build());
    }

    public static Call newCall(OkHttpClient client, String url, RequestBody body) {
        return client.newCall(new Request.Builder().url(url).post(body).build());
    }

    public static FormBody toBody(ArrayMap<String, String> params) {
        FormBody.Builder body = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) body.add(entry.getKey(), entry.getValue());
        return body.build();
    }

    private static HttpUrl buildUrl(String url, ArrayMap<String, String> params) {
        HttpUrl.Builder builder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) builder.addQueryParameter(entry.getKey(), entry.getValue());
        return builder.build();
    }

    private static OkHttpClient.Builder getBuilder() {
        return new OkHttpClient.Builder().addInterceptor(new DeflateInterceptor()).connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS).writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS).dns(dns()).hostnameVerifier(SSLCompat.VERIFIER).sslSocketFactory(new SSLCompat(), SSLCompat.TM);
    }

    private static OkHttpClient.Builder getBuilder(Uri uri) {
        String userInfo = uri.getUserInfo();
        OkHttpClient.Builder builder = client().newBuilder();
        if (userInfo != null && userInfo.contains(":")) setAuthenticator(builder, userInfo);
        if (uri.getScheme() == null || uri.getHost() == null || uri.getPort() <= 0) return builder;
        if (uri.getScheme().startsWith("http")) builder.proxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(uri.getHost(), uri.getPort())));
        if (uri.getScheme().startsWith("socks")) builder.proxy(new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(uri.getHost(), uri.getPort())));
        return builder;
    }

    private static void setAuthenticator(OkHttpClient.Builder builder, String userInfo) {
        builder.proxyAuthenticator((route, response) -> {
            String credential = Credentials.basic(userInfo.split(":")[0], userInfo.split(":")[1]);
            return response.request().newBuilder().header("Proxy-Authorization", credential).build();
        });
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userInfo.split(":")[0], userInfo.split(":")[1].toCharArray());
            }
        });
    }
}
