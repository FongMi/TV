package com.github.catvod.net.interceptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.catvod.Proxy;
import com.github.catvod.utils.Util;
import com.google.common.net.HttpHeaders;

import java.io.IOException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;

public class DefaultInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Response response = chain.proceed(getRequest(chain.request()));
        String encoding = response.header(HttpHeaders.CONTENT_ENCODING);
        if (response.body() == null || encoding == null || !encoding.equals("deflate")) return response;
        InflaterInputStream is = new InflaterInputStream(response.body().byteStream(), new Inflater(true));
        return response.newBuilder().headers(response.headers()).body(new ResponseBody() {
            @Nullable
            @Override
            public MediaType contentType() {
                return response.body().contentType();
            }

            @Override
            public long contentLength() {
                return response.body().contentLength();
            }

            @NonNull
            @Override
            public BufferedSource source() {
                return Okio.buffer(Okio.source(is));
            }
        }).build();
    }

    private Request getRequest(@NonNull Request request) {
        String url = request.url().toString();
        Request.Builder builder = request.newBuilder();
        boolean local = url.contains(":" + Proxy.getPort() + "/");
        if (url.contains("+") && local) builder.url(url.replace("+", "%2B"));
        if (url.contains("gitcode.net")) builder.addHeader(HttpHeaders.USER_AGENT, Util.CHROME);
        return builder.build();
    }
}
