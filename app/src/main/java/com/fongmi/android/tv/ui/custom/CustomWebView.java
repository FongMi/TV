package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.http.SslError;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.ParseTask;
import com.fongmi.android.tv.utils.Utils;

import java.util.HashMap;
import java.util.Map;

public class CustomWebView extends WebView {

    private ParseTask.Callback callback;
    private int retry;

    public CustomWebView(@NonNull Context context) {
        super(context);
        initSettings();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initSettings() {
        getSettings().setUseWideViewPort(true);
        getSettings().setDatabaseEnabled(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        setWebViewClient(webViewClient());
    }

    private void setUserAgent(Map<String, String> headers) {
        for (String key : headers.keySet()) {
            if (key.equalsIgnoreCase("user-agent")) {
                getSettings().setUserAgentString(headers.get(key));
                break;
            }
        }
    }

    public void start(String url, Map<String, String> headers, ParseTask.Callback callback) {
        this.callback = callback;
        setUserAgent(headers);
        loadUrl(url, headers);
        retry = 0;
    }

    private WebViewClient webViewClient() {
        return new WebViewClient() {
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                App.post(mTimer, 15 * 1000);
                if (Utils.isVideoFormat(url)) post(url);
                return super.shouldInterceptRequest(view, url);
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
            if (retry > 3) return;
            if (retry++ == 3) stop(true);
            else reload();
        }
    };

    private void post(String url) {
        Map<String, String> news = new HashMap<>();
        String cookie = CookieManager.getInstance().getCookie(url);
        if (!TextUtils.isEmpty(cookie)) news.put("cookie", cookie);
        App.removeCallbacks(mTimer);
        App.post(() -> {
            if (callback != null) callback.onParseSuccess(news, url, "");
            stop(false);
        });
    }

    public void stop(boolean error) {
        stopLoading();
        loadUrl("about:blank");
        App.removeCallbacks(mTimer);
        if (error) App.post(() -> callback.onParseError());
        else callback = null;
    }
}
