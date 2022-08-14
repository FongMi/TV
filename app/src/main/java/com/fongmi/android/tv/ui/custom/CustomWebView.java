package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.utils.Utils;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomWebView extends WebView {

    private WebResourceResponse empty;
    private List<String> keys;
    private String ads;

    public CustomWebView(@NonNull Context context) {
        super(context);
        initSettings();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initSettings() {
        this.ads = ApiConfig.get().getAds();
        this.keys = Arrays.asList("user-agent", "referer", "origin");
        this.empty = new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
        getSettings().setUseWideViewPort(true);
        getSettings().setDatabaseEnabled(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setBlockNetworkImage(true);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        setWebViewClient(webViewClient());
    }

    public void start(String url) {
        stopLoading();
        loadUrl(url);
    }

    private WebViewClient webViewClient() {
        return new WebViewClient() {
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                String host = request.getUrl().getHost();
                if (ads.contains(host)) return empty;
                Map<String, String> headers = request.getRequestHeaders();
                if (Utils.isVideoFormat(url) || headers.containsKey("Range")) Players.get().setMediaSource(get(headers), url);
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        };
    }

    private Map<String, String> get(Map<String, String> headers) {
        Map<String, String> news = new HashMap<>();
        for (String key : headers.keySet()) if (keys.contains(key.toLowerCase())) news.put(key, headers.get(key));
        return news;
    }

    public void stop() {
        stopLoading();
        loadUrl("about:blank");
    }
}
