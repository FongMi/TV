package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.impl.ParseCallback;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.dialog.CloudflareDialog;
import com.fongmi.android.tv.utils.Sniffer;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.google.common.net.HttpHeaders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

public final class ParseWebView extends WebView implements DialogInterface.OnDismissListener {

    private static final String TAG = "ParseWebView";

    private static final Pattern PLAYER = Pattern.compile("player.*https?://");
    private static final String BLANK = "about:blank";
    private static final int MAX_URLS = 5;

    private final AtomicReference<ParseCallback> callbackRef = new AtomicReference<>();
    private final List<ParseWebView> children = new ArrayList<>();
    private final LinkedHashSet<String> urls = new LinkedHashSet<>();
    private final Runnable timer = () -> stop(true);
    private CloudflareDialog dialog;
    private volatile String userAgent;
    private boolean destroyed;
    private boolean stopped;
    private volatile boolean loadFailed;
    private boolean detect;
    private int pageGeneration;
    private String clickScript;
    private String from;
    private String key;
    private String url;

    public static ParseWebView create(@NonNull Context context) {
        return new ParseWebView(context);
    }

    private ParseWebView(@NonNull Context context) {
        super(context);
        initSettings();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initSettings() {
        WebSettings setting = getSettings();
        setting.setSupportZoom(true);
        setting.setUseWideViewPort(true);
        setting.setDatabaseEnabled(true);
        setting.setDomStorageEnabled(true);
        setting.setJavaScriptEnabled(true);
        setting.setBuiltInZoomControls(true);
        setting.setDisplayZoomControls(false);
        setting.setLoadWithOverviewMode(true);
        setting.setUserAgentString(Setting.getUa());
        userAgent = setting.getUserAgentString();
        setting.setMediaPlaybackRequiresUserGesture(false);
        setting.setJavaScriptCanOpenWindowsAutomatically(false);
        setting.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        setWebViewClient(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new OreoClient() : new Client());
    }

    public ParseWebView start(String key, String from, Map<String, String> headers, String url, String click, ParseCallback callback, boolean detect) {
        SpiderDebug.log(TAG, "key=%s, from=%s, click=%s, url=%s, headers=%s", key, from, click, url, headers);
        App.post(timer, Constant.TIMEOUT_PARSE_WEB);
        callbackRef.set(callback);
        this.detect = detect;
        this.clickScript = click;
        this.from = from;
        this.key = key;
        this.url = url;
        Map<String, String> safeHeaders = headers == null ? Collections.emptyMap() : headers;
        WebViewClicker.ensureViewport(this);
        setHeaders(safeHeaders, url);
        loadUrl(url, safeHeaders);
        return this;
    }

    private void setHeaders(Map<String, String> headers, String url) {
        CookieManager manager = CookieManager.getInstance();
        manager.setAcceptCookie(true);
        manager.setAcceptThirdPartyCookies(this, true);
        for (Map.Entry<String, String> entry : headers.entrySet()) applyHeader(entry, url);
    }

    private void applyHeader(Map.Entry<String, String> entry, String url) {
        String name = entry.getKey();
        if (HttpHeaders.USER_AGENT.equalsIgnoreCase(UrlUtil.fixHeader(name))) {
            userAgent = entry.getValue();
            getSettings().setUserAgentString(userAgent);
        } else if (HttpHeaders.COOKIE.equalsIgnoreCase(name)) {
            WebViewCookies.set(this, url, entry.getValue());
        }
    }

    private class Client extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            pageGeneration++;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (callbackRef.get() == null || loadFailed) return super.shouldInterceptRequest(view, request);
            Uri uri = request.getUrl();
            String url = uri.toString();
            String host = uri.getHost();
            if (isLocalDocument(uri)) return super.shouldInterceptRequest(view, request);
            boolean cloudflare = WebViewCloudflare.isResource(uri);
            if (TextUtils.isEmpty(host) || (!cloudflare && WebViewRules.isAd(uri))) return WebViewRules.empty();
            Map<String, String> headers = request.getRequestHeaders();
            if (WebViewCloudflare.isTurnstile(uri)) App.post(ParseWebView.this::showDialog);
            if (detect && PLAYER.matcher(url).find() && addUrl(url)) {
                onParseAdd(headers, url);
            } else if (isVideoFormat(url)) {
                App.post(() -> {
                    if (!loadFailed) onParseSuccess(headers, url);
                });
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) onLoadError("network", error.getErrorCode());
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            if (WebViewCloudflare.isChallenge(errorResponse)) App.post(ParseWebView.this::showDialog);
            else if (request.isForMainFrame()) onLoadError("HTTP", errorResponse.getStatusCode());
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (BLANK.equals(url) || stopped || loadFailed) return;
            int generation = pageGeneration;
            BooleanSupplier current = () -> !stopped && !loadFailed && generation == pageGeneration;
            Uri uri = Uri.parse(url);
            evaluateJavascript(WebViewCloudflare.CHECK_SCRIPT, value -> {
                if (!current.getAsBoolean()) return;
                if ("true".equalsIgnoreCase(value)) showDialog();
                else WebViewRules.apply(ParseWebView.this, uri, clickScript, current);
            });
        }

        @Override
        @SuppressLint("WebViewClientOnReceivedSslError")
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return false;
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private final class OreoClient extends Client {

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            stop(true, true);
            destroyView();
            return true;
        }
    }

    private static boolean isLocalDocument(Uri uri) {
        return "about".equalsIgnoreCase(uri.getScheme()) || "data".equalsIgnoreCase(uri.getScheme());
    }

    private synchronized boolean addUrl(String url) {
        return urls.size() < MAX_URLS && urls.add(url);
    }

    private void showDialog() {
        Activity activity = App.activity();
        if (stopped || callbackRef.get() == null || dialog != null || activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (getParent() != null) ((ViewGroup) getParent()).removeView(this);
        dialog = CloudflareDialog.create(activity, this, this).show();
        App.removeCallbacks(timer);
    }

    private void hideDialog() {
        CloudflareDialog current = dialog;
        dialog = null;
        if (current != null) current.dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        this.dialog = null;
        stop(true);
    }

    private boolean isVideoFormat(String url) {
        try {
            if (!detect && url.equals(this.url)) return false;
            Spider spider = VodConfig.get().getSite(key).spider();
            if (spider.manualVideoCheck()) return spider.isVideoFormat(url);
            return Sniffer.isVideoFormat(url);
        } catch (Exception ignored) {
            return Sniffer.isVideoFormat(url);
        }
    }

    private void onParseAdd(Map<String, String> headers, String url) {
        if (callbackRef.get() == null) return;
        App.post(() -> startChild(headers, url));
    }

    private void startChild(Map<String, String> headers, String url) {
        if (stopped || loadFailed || callbackRef.get() == null) return;
        ParseWebView child = ParseWebView.create(App.get());
        children.add(child);
        child.start(key, from, headers, url, clickScript, new ParseCallback() {
            @Override
            public void onParseSuccess(Map<String, String> headers, String url, String from) {
                ParseWebView.this.onParseSuccess(headers, url);
            }

            @Override
            public void onParseError() {
                children.remove(child);
                child.release();
                if (loadFailed && children.isEmpty()) stop(true);
            }
        }, false);
    }

    private void onLoadError(String kind, int code) {
        if (stopped || loadFailed || callbackRef.get() == null) return;
        SpiderDebug.log(TAG, "load error=%s %d", kind, code);
        loadFailed = true;
        stopLoading();
        if (children.isEmpty()) stop(true);
    }

    private void onParseSuccess(Map<String, String> headers, String url) {
        ParseCallback cb = callbackRef.getAndSet(null);
        if (cb == null) return;
        try {
            Uri uri = Uri.parse(url);
            SpiderDebug.log(TAG, "success=%s://%s%s", uri.getScheme(), uri.getHost(), uri.getPath());
            cb.onParseSuccess(getResultHeaders(headers, url), url, from);
        } finally {
            stop(false);
        }
    }

    private Map<String, String> getResultHeaders(Map<String, String> headers, String url) {
        Map<String, String> result = new LinkedHashMap<>(headers == null ? Collections.emptyMap() : headers);
        putHeader(result, HttpHeaders.COOKIE, CookieManager.getInstance().getCookie(url));
        putHeader(result, HttpHeaders.USER_AGENT, userAgent);
        return result;
    }

    private static void putHeader(Map<String, String> headers, String name, String value) {
        if (TextUtils.isEmpty(value)) return;
        headers.keySet().removeIf(name::equalsIgnoreCase);
        headers.put(name, value);
    }

    private void stop(boolean error) {
        stop(error, false);
    }

    private void stop(boolean error, boolean rendererGone) {
        if (stopped) return;
        ParseCallback cb = callbackRef.getAndSet(null);
        stopped = true;
        hideDialog();
        if (!rendererGone) {
            stopLoading();
            loadUrl(BLANK);
        }
        App.removeCallbacks(timer);
        for (ParseWebView child : children) child.release();
        children.clear();
        synchronized (this) {
            urls.clear();
        }
        if (error && cb != null) cb.onParseError();
    }

    public void release() {
        stop(false);
        destroyView();
    }

    private void destroyView() {
        if (destroyed) return;
        destroyed = true;
        destroy();
    }
}
