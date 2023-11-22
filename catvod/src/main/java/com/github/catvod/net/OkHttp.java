package com.github.catvod.net;

import android.net.Uri;

import androidx.collection.ArrayMap;

import com.github.catvod.bean.Doh;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Util;
import com.google.common.net.HttpHeaders;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.ConnectionSpec;
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
    private ProxySelector selector;

    private static class Loader {
        static volatile OkHttp INSTANCE = new OkHttp();
    }

    public static OkHttp get() {
        return Loader.INSTANCE;
    }

    public static Dns dns() {
        return get().dns != null ? get().dns : Dns.SYSTEM;
    }

    public void setDoh(Doh doh) {
        OkHttpClient dohClient = new OkHttpClient.Builder().cache(new Cache(Path.doh(), CACHE)).build();
        dns = doh.getUrl().isEmpty() ? null : new DnsOverHttps.Builder().client(dohClient).url(HttpUrl.get(doh.getUrl())).bootstrapDnsHosts(doh.getHosts()).build();
        client = null;
    }

    public void setProxy(String proxy) {
        ProxySelector.setDefault(selector());
        selector().setProxy(proxy);
        client = null;
    }

    public static ProxySelector selector() {
        if (get().selector != null) return get().selector;
        return get().selector = new ProxySelector();
    }

    public static OkHttpClient client() {
        if (get().client != null) return get().client;
        return get().client = getBuilder().build();
    }

    public static OkHttpClient client(int timeout) {
        return client().newBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).writeTimeout(timeout, TimeUnit.MILLISECONDS).build();
    }

    public static OkHttpClient noRedirect(int timeout) {
        return client().newBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).writeTimeout(timeout, TimeUnit.MILLISECONDS).followRedirects(false).followSslRedirects(false).build();
    }

    public static OkHttpClient client(boolean redirect, int timeout) {
        return redirect ? client(timeout) : noRedirect(timeout);
    }

    public static String string(String url) {
        try {
            return url.startsWith("http") ? newCall(url).execute().body().string() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static Call newCall(String url) {
        Uri uri = Uri.parse(url);
        if (uri.getUserInfo() != null) return newCall(url, Headers.of(HttpHeaders.AUTHORIZATION, Util.basic(uri)));
        return client().newCall(new Request.Builder().url(url).build());
    }

    public static Call newCall(OkHttpClient client, String url) {
        return client.newCall(new Request.Builder().url(url).build());
    }

    public static Call newCall(OkHttpClient client, String url, Headers headers) {
        return client.newCall(new Request.Builder().url(url).headers(headers).build());
    }

    public static Call newCall(String url, Headers headers) {
        return client().newCall(new Request.Builder().url(url).headers(headers).build());
    }

    public static Call newCall(String url, Headers headers, ArrayMap<String, String> params) {
        return client().newCall(new Request.Builder().url(buildUrl(url, params)).headers(headers).build());
    }

    public static Call newCall(String url, Headers headers, RequestBody body) {
        return client().newCall(new Request.Builder().url(url).headers(headers).post(body).build());
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
        OkHttpClient.Builder builder = new OkHttpClient.Builder().connectionSpecs(Arrays.asList(ConnectionSpec.RESTRICTED_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT)).addInterceptor(new OkhttpInterceptor()).connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(TIMEOUT, TimeUnit.MILLISECONDS).writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS).dns(dns());
        builder.proxySelector(selector());
        ignoreSSLErrors(builder);
        return builder;
    }

    private static void ignoreSSLErrors(OkHttpClient.Builder builder) {
        try {
            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, TM, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            builder.sslSocketFactory(context.getSocketFactory(), (X509TrustManager) TM[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final TrustManager[] TM = new TrustManager[]{new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    }};
}
