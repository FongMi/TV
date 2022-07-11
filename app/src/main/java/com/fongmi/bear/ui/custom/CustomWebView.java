package com.fongmi.bear.ui.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.bear.player.Player;
import com.fongmi.bear.utils.Utils;

import java.util.HashMap;
import java.util.Map;

public class CustomWebView extends WebView {

    public CustomWebView(@NonNull Context context) {
        super(context);
    }

    public CustomWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public CustomWebView init() {
        getSettings().setUseWideViewPort(true);
        getSettings().setDatabaseEnabled(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setBlockNetworkImage(true);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        setWebViewClient(webViewClient());
        return this;
    }

    private WebViewClient webViewClient() {
        return new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                HashMap<String, String> headers = new HashMap<>();
                Map<String, String> hds = request.getRequestHeaders();
                for (String k : hds.keySet()) {
                    if (k.equalsIgnoreCase("user-agent") || k.equalsIgnoreCase("referer") || k.equalsIgnoreCase("origin")) {
                        headers.put(k, hds.get(k));
                    }
                }
                if (Utils.isVideoFormat(url)) {
                    if (headers.isEmpty()) {
                        new Handler(Looper.getMainLooper()).post(() -> destroy());
                        Player.get().setMediaSource(headers, url);
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> destroy());
                        Player.get().setMediaSource(new HashMap<>(), url);
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        };
    }
}
