package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
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
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.player.parse.ParseJob;
import com.fongmi.android.tv.utils.Utils;
import com.github.catvod.crawler.Spider;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class CustomWebView extends WebView {

    private ParseJob.Callback callback;
    private WebResourceResponse empty;
    private Runnable timer;
    private String key;

    public static CustomWebView create(@NonNull Context context) {
        return new CustomWebView(context);
    }

    public CustomWebView(@NonNull Context context) {
        super(context);
        initSettings();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initSettings() {
        this.timer = () -> stop(true);
        this.empty = new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
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

    public CustomWebView start(String key, String url, Map<String, String> headers, ParseJob.Callback callback) {
        App.post(timer, Constant.TIMEOUT_PARSE_WEB);
        this.callback = callback;
        setUserAgent(headers);
        loadUrl(url, headers);
        this.key = key;
        return this;
    }

    private WebViewClient webViewClient() {
        return new WebViewClient() {
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (isAds(url)) return empty;
                if (isVideoFormat(url)) post(url);
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            @SuppressLint("WebViewClientOnReceivedSslError")
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        };
    }

    private boolean isAds(String url) {
        try {
            return ApiConfig.get().getAds().contains(Uri.parse(url).getHost());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isVideoFormat(String url) {
        try {
            Site site = ApiConfig.get().getSite(key);
            Spider spider = ApiConfig.get().getCSP(site);
            if (spider.manualVideoCheck()) return spider.isVideoFormat(url);
            return Utils.isVideoFormat(url);
        } catch (Exception ignored) {
            return Utils.isVideoFormat(url);
        }
    }

    private void post(String url) {
        Map<String, String> news = new HashMap<>();
        String cookie = CookieManager.getInstance().getCookie(url);
        if (!TextUtils.isEmpty(cookie)) news.put("cookie", cookie);
        App.post(() -> onSuccess(news, url));
    }

    public void stop(boolean error) {
        stopLoading();
        loadUrl("about:blank");
        App.removeCallbacks(timer);
        if (error) App.post(this::onError);
        else callback = null;
    }

    private void onSuccess(Map<String, String> news, String url) {
        if (callback != null) callback.onParseSuccess(news, url, "");
        callback = null;
        stop(false);
    }

    private void onError() {
        if (callback != null) callback.onParseError();
        callback = null;
    }
}
