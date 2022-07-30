package com.fongmi.android.tv.net;

import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class OKHttp {

    private final OkHttpClient mOk;

    private static class Loader {
        static volatile OKHttp INSTANCE = new OKHttp();
    }

    public static OKHttp get() {
        return Loader.INSTANCE;
    }

    public OKHttp() {
        mOk = getBuilder().build();
    }

    private OkHttpClient.Builder getBuilder() {
        return new OkHttpClient.Builder().readTimeout(5, TimeUnit.SECONDS).writeTimeout(5, TimeUnit.SECONDS).connectTimeout(5, TimeUnit.SECONDS).sslSocketFactory(new SSLSocketFactoryCompat(SSLSocketFactoryCompat.trustAllCert), SSLSocketFactoryCompat.trustAllCert);
    }

    private OkHttpClient client() {
        return mOk;
    }

    public static <T> Call newCall(T url) {
        if (url instanceof HttpUrl) return get().client().newCall(new Request.Builder().url((HttpUrl) url).build());
        else return get().client().newCall(new Request.Builder().url((String) url).build());
    }
}
