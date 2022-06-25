package com.fongmi.bear.net;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class OKHttp {

    private final OkHttpClient mClient;

    private static class Loader {
        static volatile OKHttp INSTANCE = new OKHttp();
    }

    public static OKHttp get() {
        return Loader.INSTANCE;
    }

    public OKHttp() {
        mClient = getBuilder().build();
    }

    private OkHttpClient.Builder getBuilder() {
        return new OkHttpClient.Builder().readTimeout(15, TimeUnit.SECONDS).writeTimeout(15, TimeUnit.SECONDS).connectTimeout(15, TimeUnit.SECONDS).retryOnConnectionFailure(true).sslSocketFactory(new SSLSocketFactoryCompat(SSLSocketFactoryCompat.trustAllCert), SSLSocketFactoryCompat.trustAllCert);
    }

    public OkHttpClient client() {
        return mClient;
    }
}
