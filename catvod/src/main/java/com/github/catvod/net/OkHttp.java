package com.github.catvod.net;

import android.net.Uri;

import androidx.collection.ArrayMap;

import com.github.catvod.bean.Doh;
import com.github.catvod.utils.Path;
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

    private Uri proxy;
    private DnsOverHttps dns;
    private OkHttpClient client;
    private OkHttpClient noRedirect;

    private static class Loader {
        static volatile OkHttp INSTANCE = new OkHttp();
    }

    public static OkHttp get() {
        return Loader.INSTANCE;
    }

    public void setDoh(Doh doh) {
        OkHttpClient dohClient = new OkHttpClient.Builder().cache(new Cache(Path.doh(), CACHE)).hostnameVerifier(SSLCompat.VERIFIER).sslSocketFactory(new SSLCompat(), SSLCompat.TM).build();
        dns = doh.getUrl().isEmpty() ? null : new DnsOverHttps.Builder().client(dohClient).url(HttpUrl.get(doh.getUrl())).bootstrapDnsHosts(doh.getHosts()).build();
        noRedirect = null;
        client = null;
    }

    public void setProxy(String proxy) {
        Authenticator.setDefault(null);
        this.proxy = Uri.parse(proxy);
        noRedirect = null;
        client = null;
    }

    public static OkHttpClient client() {
        if (get().client != null) return get().client;
        return get().client = client(TIMEOUT);
    }

    public static OkHttpClient noRedirect() {
        if (get().noRedirect != null) return get().noRedirect;
        return get().noRedirect = client().newBuilder().followRedirects(false).followSslRedirects(false).build();
    }

    public static Dns dns() {
        return get().dns != null ? get().dns : Dns.SYSTEM;
    }

    public static Uri proxy() {
        return get().proxy;
    }

    public static OkHttpClient client(int timeout) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder().addInterceptor(new DeflateInterceptor()).connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).writeTimeout(timeout, TimeUnit.MILLISECONDS).dns(dns()).hostnameVerifier(SSLCompat.VERIFIER).sslSocketFactory(new SSLCompat(), SSLCompat.TM);
        if (proxy() != null && proxy().getScheme() != null && proxy().getHost() != null && proxy().getPort() > 0) setProxy(builder);
        return builder.build();
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

    private static void setProxy(OkHttpClient.Builder builder) {
        String userInfo = proxy().getUserInfo();
        if (userInfo != null && userInfo.contains(":")) setAuthenticator(builder, userInfo);
        if (Util.scheme(proxy()).startsWith("http")) builder.proxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxy().getHost(), proxy().getPort())));
        if (Util.scheme(proxy()).startsWith("socks")) builder.proxy(new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(proxy().getHost(), proxy().getPort())));
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
