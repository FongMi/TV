package com.fongmi.bear.net;

import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.dnsoverhttps.DnsOverHttps;

public class OKHttp {

    private OkHttpClient mOk;
    private DnsOverHttps mDns;

    private static class Loader {
        static volatile OKHttp INSTANCE = new OKHttp();
    }

    public static OKHttp get() {
        return Loader.INSTANCE;
    }

    public OKHttp() {
        init();
    }

    private OkHttpClient.Builder getBuilder() {
        return new OkHttpClient.Builder().dns(mDns).readTimeout(5, TimeUnit.SECONDS).writeTimeout(5, TimeUnit.SECONDS).connectTimeout(5, TimeUnit.SECONDS).sslSocketFactory(new SSLSocketFactoryCompat(SSLSocketFactoryCompat.trustAllCert), SSLSocketFactoryCompat.trustAllCert);
    }

    private void init() {
        mOk = getBuilder().build();
        mDns = new DnsOverHttps.Builder().client(mOk).url(null).build();
    }

    private OkHttpClient client() {
        return mOk;
    }

    public DnsOverHttps getDns() {
        return mDns;
    }

    public static <T> Call newCall(T url) {
        if (url instanceof HttpUrl) return get().client().newCall(new Request.Builder().url((HttpUrl) url).build());
        else return get().client().newCall(new Request.Builder().url((String) url).build());
    }
}
