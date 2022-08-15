package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.event.PlayerEvent;
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
    private Handler handler;
    private String ads;
    private int retry;

    public CustomWebView(@NonNull Context context) {
        super(context);
        initSettings();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initSettings() {
        this.ads = ApiConfig.get().getAds();
        this.keys = Arrays.asList("user-agent", "referer", "origin");
        this.empty = new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
        this.handler = new Handler(Looper.getMainLooper());
        getSettings().setUseWideViewPort(true);
        getSettings().setDatabaseEnabled(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setBlockNetworkImage(true);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setLoadsImagesAutomatically(false);
        getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        setWebViewClient(webViewClient());
    }

    public void start(String url) {
        stopLoading();
        loadUrl(url);
        retry = 0;
    }

    private WebViewClient webViewClient() {
        return new WebViewClient() {
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                String host = request.getUrl().getHost();
                if (ads.contains(host)) return empty;
                handler.removeCallbacks(mTimer);
                handler.postDelayed(mTimer, 5000);
                Map<String, String> headers = request.getRequestHeaders();
                if (Utils.isVideoFormat(url) || headers.containsKey("Range")) post(get(headers), url);
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

    private final Runnable mTimer = new Runnable() {
        @Override
        public void run() {
            if (retry > 5) return;
            if (retry++ == 5) stop(true);
            else reload();
        }
    };

    private Map<String, String> get(Map<String, String> headers) {
        Map<String, String> news = new HashMap<>();
        for (String key : headers.keySet()) if (keys.contains(key.toLowerCase())) news.put(key, headers.get(key));
        return news;
    }

    private void post(Map<String, String> headers, String url) {
        handler.removeCallbacks(mTimer);
        handler.post(() -> {
            stop(false);
            Players.get().setMediaSource(headers, url);
        });
    }

    public void stop(boolean error) {
        stopLoading();
        loadUrl("about:blank");
        if (error) PlayerEvent.error(R.string.error_play_parse);
    }
}
