package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.http.SslError;
import android.webkit.ValueCallback;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.player.ParseTask;
import com.fongmi.android.tv.utils.Utils;
import com.github.catvod.crawler.Spider;

import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkWebResourceRequest;
import org.xwalk.core.XWalkWebResourceResponse;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomWebView extends XWalkView {

    private ParseTask.Callback callback;
    private List<String> keys;
    private String key;
    private String ads;
    private int retry;

    public static CustomWebView create(@NonNull Context context) {
        return new CustomWebView(context);
    }

    public CustomWebView(@NonNull Context context) {
        super(context);
        initSettings();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void initSettings() {
        this.ads = ApiConfig.get().getAds();
        this.keys = Arrays.asList("user-agent", "referer", "origin");
        setResourceClient(webViewClient());
        getSettings().setAllowFileAccess(true);
        getSettings().setUseWideViewPort(true);
        getSettings().setDatabaseEnabled(true);
        getSettings().setDomStorageEnabled(true);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setAllowContentAccess(true);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setAllowFileAccessFromFileURLs(true);
        getSettings().setAllowUniversalAccessFromFileURLs(true);
        getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
    }

    private void setUserAgent(Map<String, String> headers) {
        for (String key : headers.keySet()) {
            if (key.equalsIgnoreCase("user-agent")) {
                getSettings().setUserAgentString(headers.get(key));
                break;
            }
        }
    }

    public CustomWebView start(String key, String url, Map<String, String> headers, ParseTask.Callback callback) {
        this.callback = callback;
        setUserAgent(headers);
        loadUrl(url, headers);
        this.key = key;
        return this;
    }

    private XWalkResourceClient webViewClient() {
        return new XWalkResourceClient(this) {
            @Override
            public XWalkWebResourceResponse shouldInterceptLoadRequest(XWalkView view, XWalkWebResourceRequest request) {
                String url = request.getUrl().toString();
                String host = request.getUrl().getHost();
                if (ads.contains(host)) return createXWalkWebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
                App.post(mTimer, 15 * 1000);
                Map<String, String> headers = request.getRequestHeaders();
                if (isVideoFormat(url, headers)) post(headers, url);
                return super.shouldInterceptLoadRequest(view, request);
            }

            @Override
            public void onReceivedSslError(XWalkView view, ValueCallback<Boolean> callback, SslError error) {
                callback.onReceiveValue(true);
            }

            @Override
            public boolean shouldOverrideUrlLoading(XWalkView view, String url) {
                return false;
            }
        };
    }

    private boolean isVideoFormat(String url, Map<String, String> headers) {
        Site site = ApiConfig.get().getSite(key);
        Spider spider = ApiConfig.get().getCSP(site);
        if (spider.manualVideoCheck()) return spider.isVideoFormat(url);
        return Utils.isVideoFormat(url, headers);
    }

    private final Runnable mTimer = new Runnable() {
        @Override
        public void run() {
            if (retry > 3) return;
            if (retry++ == 3) stop(true);
            else reload(RELOAD_NORMAL);
        }
    };

    private void post(Map<String, String> headers, String url) {
        Map<String, String> news = new HashMap<>();
        for (String key : headers.keySet()) if (keys.contains(key.toLowerCase())) news.put(key, headers.get(key));
        App.removeCallbacks(mTimer);
        App.post(() -> {
            onSuccess(news, url);
            stop(false);
        });
    }

    public void stop(boolean error) {
        stopLoading();
        loadUrl("about:blank");
        App.removeCallbacks(mTimer);
        if (error) App.post(this::onError);
        else callback = null;
    }

    private void onSuccess(Map<String, String> news, String url) {
        if (callback != null) callback.onParseSuccess(news, url, "");
        callback = null;
    }

    private void onError() {
        if (callback != null) callback.onParseError();
        callback = null;
    }
}
