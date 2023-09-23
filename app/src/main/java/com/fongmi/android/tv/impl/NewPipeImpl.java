package com.fongmi.android.tv.impl;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.utils.Sniffer;
import com.github.catvod.net.OkHttp;
import com.google.common.net.HttpHeaders;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.util.List;
import java.util.Map;


import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class NewPipeImpl extends Downloader {

    @Override
    public Response execute(@NonNull Request request) throws IOException, ReCaptchaException {
        String url = request.url();
        RequestBody reqBody = null;
        byte[] dataToSend = request.dataToSend();
        if (dataToSend != null) reqBody = RequestBody.create(dataToSend, null);
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder().method(request.httpMethod(), reqBody).url(url).addHeader(HttpHeaders.USER_AGENT, Sniffer.CHROME);
        for (Map.Entry<String, List<String>> pair : request.headers().entrySet()) {
            String headerName = pair.getKey();
            List<String> headerValueList = pair.getValue();
            if (headerValueList.size() > 1) {
                builder.removeHeader(headerName);
                for (String headerValue : headerValueList) builder.addHeader(headerName, headerValue);
            } else if (headerValueList.size() == 1) {
                builder.header(headerName, headerValueList.get(0));
            }
        }
        okhttp3.Response response = OkHttp.client().newCall(builder.build()).execute();
        if (response.code() == 429) {
            response.close();
            throw new ReCaptchaException("reCaptcha Challenge requested", url);
        }
        String responseBodyToReturn = null;
        ResponseBody resBody = response.body();
        if (resBody != null) responseBodyToReturn = resBody.string();
        String latestUrl = response.request().url().toString();
        return new Response(response.code(), response.message(), response.headers().toMultimap(), responseBodyToReturn, latestUrl);
    }
}
