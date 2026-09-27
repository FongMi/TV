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
        CookieManager manager = CookieManager.getInstance();
        manager.setAcceptCookie(true);
        manager.setAcceptThirdPartyCookies(this, true);
        for (Map.Entry<String, String> entry : safeHeaders.entrySet()) {
            String name = entry.getKey();
            if (HttpHeaders.USER_AGENT.equalsIgnoreCase(UrlUtil.fixHeader(name))) {
                userAgent = entry.getValue();
                getSettings().setUserAgentString(userAgent);
            } else if (HttpHeaders.COOKIE.equalsIgnoreCase(name)) WebViewCookies.set(this, url, entry.getValue());
        }
        loadUrl(url, safeHeaders);
        return this;
    }

    private class Client extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            pageGeneration++;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String url = uri.toString();
            String host = uri.getHost();
            if (isLocalDocument(uri)) return super.shouldInterceptRequest(view, request);
            boolean cloudflare = WebViewCloudflare.isResource(uri);
            if (TextUtils.isEmpty(host) || (!cloudflare && WebViewRules.isAd(uri))) return WebViewRules.empty();
            Map<String, String> headers = request.getRequestHeaders();
            if (WebViewCloudflare.isTurnstile(uri)) App.post(ParseWebView.this::showDialog);
            if (detect && PLAYER.matcher(url).find() && addUrl(url)) onParseAdd(headers, url);
            else if (isVideoFormat(url)) onParseSuccess(headers, url);
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            if (WebViewCloudflare.isChallenge(errorResponse)) App.post(ParseWebView.this::showDialog);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (BLANK.equals(url)) return;
            int generation = pageGeneration;
            BooleanSupplier current = () -> !stopped && generation == pageGeneration;
            Uri uri = Uri.parse(url);
            checkCloudflareChallenge(current, () -> WebViewRules.apply(ParseWebView.this, uri, clickScript, current));
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

    private void checkCloudflareChallenge(BooleanSupplier current, Runnable ready) {
        evaluateJavascript(WebViewCloudflare.CHECK_SCRIPT, value -> {
            if (!current.getAsBoolean()) return;
            if ("true".equalsIgnoreCase(value)) showDialog();
            else ready.run();
        });
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
        ParseCallback cb = callbackRef.get();
        if (cb == null) return;
        post(() -> {
            if (stopped || callbackRef.get() == null) return;
            children.add(ParseWebView.create(App.get()).start(key, from, headers, url, clickScript, cb, false));
        });
    }

    private void onParseSuccess(Map<String, String> headers, String url) {
        Uri uri = Uri.parse(url);
        SpiderDebug.log(TAG, "success=%s://%s%s", uri.getScheme(), uri.getHost(), uri.getPath());
        Map<String, String> result = new LinkedHashMap<>(headers == null ? Collections.emptyMap() : headers);
        putHeader(result, HttpHeaders.COOKIE, CookieManager.getInstance().getCookie(url));
        putHeader(result, HttpHeaders.USER_AGENT, userAgent);
        ParseCallback cb = callbackRef.getAndSet(null);
        if (cb != null) cb.onParseSuccess(result, url, from);
        post(() -> stop(false));
    }

    private static void putHeader(Map<String, String> headers, String name, String value) {
        if (TextUtils.isEmpty(value)) return;
        headers.keySet().removeIf(name::equalsIgnoreCase);
        headers.put(name, value);
    }

    private void onParseError() {
        ParseCallback cb = callbackRef.getAndSet(null);
        if (cb != null) cb.onParseError();
    }

    private void releaseChildren() {
        for (ParseWebView child : children) child.release();
        children.clear();
    }

    private void stop(boolean error) {
        stop(error, false);
    }

    private void stop(boolean error, boolean rendererGone) {
        if (stopped) return;
        stopped = true;
        hideDialog();
        if (!rendererGone) {
            stopLoading();
            loadUrl(BLANK);
        }
        App.removeCallbacks(timer);
        releaseChildren();
        urls.clear();
        if (error) onParseError();
        else callbackRef.set(null);
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
