package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.SystemClock;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.setting.Setting;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Util;
import com.google.common.net.HttpHeaders;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressLint("ViewConstructor")
public final class BrowserWebView extends WebView {

    public interface BrowserListener {

        void onShowFileChooser(@NonNull ValueCallback<Uri[]> callback, @NonNull WebChromeClient.FileChooserParams params);

        void onDownload(@NonNull Download download);

        void onDownloadError();

        default void onRendererGone(@NonNull BrowserWebView view) {
        }

        default void onOpenExternal(@NonNull Uri uri) {
        }

        default void onFullscreenChanged(@NonNull BrowserWebView view, boolean fullscreen) {
        }

        default void onPageReady(@NonNull BrowserWebView view) {
        }
    }

    public record Download(@NonNull String fileName, @NonNull String mimeType, @NonNull byte[] data) {
    }

    public interface PlaybackListener {

        void onPlayingChanged(@NonNull BrowserWebView view, boolean isPlaying);

        void onStateChanged(@NonNull BrowserWebView view, @NonNull PlaybackState state);

        void onFailure(@NonNull BrowserWebView view, boolean rendererGone);

        default void onFullscreenChanged(@NonNull BrowserWebView view, boolean fullscreen) {
        }

        default void onChallengeChanged(@NonNull BrowserWebView view, boolean visible) {
        }
    }

    public enum PlaybackType {
        PAGE,
        VIDEO,
        FRAME
    }

    public record PlaybackState(PlaybackType type, boolean playing, boolean buffering, boolean ended, int width, int height, long positionMs, long durationMs, boolean started) {

        private static PlaybackState from(String value) throws JSONException {
            JSONArray array = new JSONArray(value);
            int index = array.optInt(0, -1);
            if (index < 0 || index >= PlaybackType.values().length) throw new JSONException("Unknown playback type");
            PlaybackType type = PlaybackType.values()[index];
            return new PlaybackState(type, array.getBoolean(1), array.getBoolean(2), array.getBoolean(3), array.optInt(4), array.optInt(5), Math.max(0, array.optLong(6)), array.optLong(7, -1), array.optBoolean(8));
        }

        public boolean isSeekable() {
            return type == PlaybackType.VIDEO && durationMs > 0;
        }
    }

    private static final String BLANK = "about:blank";
    private static final String TAG = "BrowserWebView";
    private static final int MAX_DOWNLOAD_BYTES = 512 * 1024;
    private static final int MAX_DOWNLOAD_BASE64_CHARS = ((MAX_DOWNLOAD_BYTES + 2) / 3) * 4;
    private static final long DOWNLOAD_POLL_DELAY = 50;
    private static final long DOWNLOAD_TIMEOUT = 10_000;
    private static final String DOWNLOAD_BRIDGE_NAME = "FongMiDownloads";
    private static final long TARGET_SCAN_DELAY = 2000;
    private static final long STARTUP_SCAN_DELAY = 250;
    private static final long STARTUP_POLL_DELAY = 100;
    private static final long STATE_POLL_DELAY = 500;

    private final Runnable targetTask = this::scanTarget;
    private final Runnable stateTask = this::pollState;
    @Nullable
    private final BrowserListener browserListener;
    @Nullable
    private final PlaybackListener listener;
    @Nullable
    private WebChromeClient.CustomViewCallback customViewCallback;
    @Nullable
    private View customView;
    private boolean released;
    private boolean targetReady;
    private boolean failureReported;
    private boolean challengeVisible;
    private boolean playbackPaused;
    private boolean playbackActive;
    private boolean awaitingPlaybackStart;
    private boolean videoPlaying;
    @Nullable
    private PlaybackState playbackState;
    private final String downloadBridgeToken = UUID.randomUUID().toString();
    private int loadGeneration;
    private int pageGeneration;
    private String clickScript;
    private String defaultUserAgent;
    @Nullable
    private String previousPlaybackUrl;
    @Nullable
    private String requestedPlaybackUrl;
    private int downloadGeneration;

    public static BrowserWebView createBrowser(@NonNull Context context, String url, @NonNull BrowserListener listener) {
        BrowserWebView view = new BrowserWebView(context, null, listener);
        view.loadUrl(url);
        return view;
    }

    public static BrowserWebView createPlayback(@NonNull Context context, @NonNull PlaybackListener listener) {
        return new BrowserWebView(context, listener, null);
    }

    private BrowserWebView(@NonNull Context context, @Nullable PlaybackListener listener, @Nullable BrowserListener browserListener) {
        super(context);
        this.listener = listener;
        this.browserListener = browserListener;
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
        String userAgent = Setting.getUa();
        if (userAgent.isEmpty() && isPlayback()) userAgent = Util.CHROME;
        if (!userAgent.isEmpty()) setting.setUserAgentString(userAgent);
        defaultUserAgent = setting.getUserAgentString();
        setting.setMediaPlaybackRequiresUserGesture(false);
        setting.setJavaScriptCanOpenWindowsAutomatically(false);
        setting.setAllowContentAccess(false);
        setting.setAllowFileAccess(false);
        setting.setMixedContentMode(isPlayback() ? WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE : WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager manager = CookieManager.getInstance();
        manager.setAcceptCookie(true);
        manager.setAcceptThirdPartyCookies(this, true);
        setWebChromeClient(new ChromeClient());
        setWebViewClient(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new OreoClient() : new Client());
        if (browserListener != null) {
            addJavascriptInterface(new DownloadBridge(), DOWNLOAD_BRIDGE_NAME);
            setDownloadListener(this::onDownloadStart);
        }
    }

    public void loadPlayback(String url, @Nullable Map<String, String> headers, @Nullable String clickScript) {
        if (released || !isPlayback()) return;
        hideCustomView();
        setVisibility(View.VISIBLE);
        previousPlaybackUrl = getUrl();
        requestedPlaybackUrl = url;
        playbackActive = true;
        awaitingPlaybackStart = true;
        loadGeneration++;
        pageGeneration++;
        this.clickScript = clickScript;
        reportChallenge(false);
        failureReported = false;
        playbackPaused = false;
        resetPlaybackState();
        stopLoading();
        loadUrl(url, prepareHeaders(url, headers));
    }

    private Map<String, String> prepareHeaders(String url, @Nullable Map<String, String> headers) {
        Map<String, String> requestHeaders = new HashMap<>();
        String userAgent = defaultUserAgent;
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key == null || value == null || value.isEmpty()) continue;
                if (HttpHeaders.USER_AGENT.equalsIgnoreCase(key) || "ua".equalsIgnoreCase(key)) userAgent = value;
                else if (HttpHeaders.COOKIE.equalsIgnoreCase(key)) WebViewCookies.set(this, url, value);
                else requestHeaders.put(key, value);
            }
        }
        getSettings().setUserAgentString(userAgent);
        return requestHeaders;
    }

    private void scanTarget() {
        int load = loadGeneration;
        int page = pageGeneration;
        if (!isCurrentPlayback(load, page) || playbackPaused) return;
        evaluateJavascript(BrowserScripts.ACTIVATE, value -> onTargetScanned(load, page, value));
    }

    private void onTargetScanned(int load, int page, String value) {
        if (!isCurrentPlayback(load, page) || playbackPaused) return;
        PlaybackState state = parseState(value);
        targetReady = state != null;
        if (state != null) {
            if (state.type() != PlaybackType.PAGE) reportChallenge(false);
            reportState(state);
            if (state.type() == PlaybackType.VIDEO && !state.ended()) scheduleStatePoll(state);
        }
        removeCallbacks(targetTask);
        postDelayed(targetTask, state != null && state.type() != PlaybackType.PAGE ? TARGET_SCAN_DELAY : STARTUP_SCAN_DELAY);
    }

    public void pauseVideo() {
        if (released || !playbackActive) return;
        playbackPaused = true;
        cancelPlaybackTasks();
        evaluateJavascript(BrowserScripts.PAUSE, null);
        reportPlaying(false);
    }

    public void stopPlayback() {
        if (released || !playbackActive) return;
        hideCustomView();
        setVisibility(View.INVISIBLE);
        clearPlaybackRequest();
        loadGeneration++;
        pageGeneration++;
        reportChallenge(false);
        playbackPaused = true;
        resetPlaybackState();
        stopLoading();
        loadUrl(BLANK);
    }

    public void resumeVideo() {
        if (released || !playbackActive || failureReported) return;
        playbackPaused = false;
        cancelPlaybackTasks();
        evaluateJavascript(BrowserScripts.PLAY, null);
        post(targetTask);
    }

    public void seekVideo(long positionMs) {
        PlaybackState state = playbackState;
        if (released || !playbackActive || state == null || !state.isSeekable()) return;
        long targetMs = Math.max(0, Math.min(positionMs, state.durationMs()));
        evaluateJavascript(BrowserScripts.SEEK.replace("%s", Double.toString(targetMs / 1000.0)), null);
    }

    public boolean isVideoPlaying() {
        return playbackActive && videoPlaying;
    }

    public boolean isPlaybackPaused() {
        return playbackPaused;
    }

    public boolean isPlaybackActive() {
        return playbackActive;
    }

    public int getVideoWidth() {
        return playbackState == null ? 0 : playbackState.width();
    }

    public int getVideoHeight() {
        return playbackState == null ? 0 : playbackState.height();
    }

    public long getVideoPosition() {
        PlaybackState state = playbackState;
        return state == null || state.type() != PlaybackType.VIDEO ? -1 : state.positionMs();
    }

    public long getVideoDuration() {
        PlaybackState state = playbackState;
        return state == null || state.type() != PlaybackType.VIDEO ? -1 : state.durationMs();
    }

    public boolean hasMediaTarget() {
        return playbackActive && playbackState != null && playbackState.type() != PlaybackType.PAGE;
    }

    public boolean handleBackNavigation() {
        if (hideCustomView()) return true;
        if (isPlayback() || !canGoBack()) return false;
        goBack();
        return true;
    }

    public boolean hideCustomView() {
        View view = customView;
        WebChromeClient.CustomViewCallback callback = customViewCallback;
        if (view == null) return false;
        customView = null;
        customViewCallback = null;
        if (view.getParent() instanceof ViewGroup parent) parent.removeView(view);
        setVisibility(View.VISIBLE);
        if (callback != null) callback.onCustomViewHidden();
        if (listener != null) listener.onFullscreenChanged(this, false);
        if (browserListener != null) browserListener.onFullscreenChanged(this, false);
        requestFocus();
        return true;
    }

    void showCustomView(@Nullable View view, @NonNull WebChromeClient.CustomViewCallback callback) {
        if (released || challengeVisible || view == null || !(getParent() instanceof ViewGroup parent)) {
            callback.onCustomViewHidden();
            return;
        }
        hideCustomView();
        if (view.getParent() instanceof ViewGroup oldParent) oldParent.removeView(view);
        customView = view;
        customViewCallback = callback;
        if (listener != null) listener.onFullscreenChanged(this, true);
        if (browserListener != null) browserListener.onFullscreenChanged(this, true);
        int index = Math.max(0, parent.indexOfChild(this) + 1);
        parent.addView(view, index, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        view.setBackgroundColor(Color.BLACK);
        setVisibility(View.INVISIBLE);
        view.requestFocus();
    }

    private boolean isCurrentLoad(int generation) {
        return !released && !failureReported && generation == loadGeneration;
    }

    private boolean isCurrentPlaybackPage(WebView view, String url) {
        if (!isPlayback() || !playbackActive || url == null) return false;
        if (awaitingPlaybackStart) return url.equals(requestedPlaybackUrl);
        return url.equals(view.getUrl());
    }

    private boolean isCurrentPlayback(int load, int page) {
        return isPlayback() && playbackActive && !awaitingPlaybackStart && isCurrentPageLoad(load, page);
    }

    private boolean isCurrentPageLoad(int load, int page) {
        return isCurrentLoad(load) && page == pageGeneration;
    }

    private boolean isCurrentPage(int load, int page, String url) {
        return isCurrentPageLoad(load, page) && url != null && url.equals(getUrl());
    }

    private boolean isPlayback() {
        return listener != null;
    }

    private void clearPlaybackRequest() {
        playbackActive = false;
        awaitingPlaybackStart = false;
        previousPlaybackUrl = null;
        requestedPlaybackUrl = null;
    }

    private void cancelPlaybackTasks() {
        removeCallbacks(targetTask);
        removeCallbacks(stateTask);
    }

    private void resetPlaybackState() {
        targetReady = false;
        playbackState = null;
        cancelPlaybackTasks();
        reportPlaying(false);
        setAlpha(0f);
    }

    private void postChallenge() {
        int load = loadGeneration;
        int page = pageGeneration;
        App.post(() -> {
            if (isCurrentPageLoad(load, page) && (!isPlayback() || (playbackActive && !awaitingPlaybackStart))) showChallenge();
        });
    }

    private void pollState() {
        int load = loadGeneration;
        int page = pageGeneration;
        if (!isCurrentPlayback(load, page) || playbackPaused || !targetReady) return;
        evaluateJavascript(BrowserScripts.STATE, value -> onStatePolled(load, page, value));
    }

    private void onStatePolled(int load, int page, String value) {
        if (!isCurrentPlayback(load, page) || playbackPaused) return;
        PlaybackState state = parseState(value);
        if (state == null) {
            targetReady = false;
            removeCallbacks(targetTask);
            post(targetTask);
            return;
        }
        reportState(state);
        if (state.type() == PlaybackType.VIDEO && !state.ended()) scheduleStatePoll(state);
    }

    private void scheduleStatePoll(@NonNull PlaybackState state) {
        removeCallbacks(stateTask);
        postDelayed(stateTask, state.started() ? STATE_POLL_DELAY : STARTUP_POLL_DELAY);
    }

    @Nullable
    private PlaybackState parseState(String value) {
        try {
            return value == null || "null".equals(value) ? null : PlaybackState.from(value);
        } catch (JSONException e) {
            return null;
        }
    }

    private void reportState(@NonNull PlaybackState state) {
        boolean changed = !state.equals(playbackState);
        playbackState = state;
        reportPlaying(state.playing());
        if (changed && listener != null) listener.onStateChanged(this, state);
    }

    private void reportPlaying(boolean isPlaying) {
        if (videoPlaying == isPlaying) return;
        videoPlaying = isPlaying;
        if (listener != null) listener.onPlayingChanged(this, isPlaying);
    }

    private void reportFailure(int generation, boolean rendererGone) {
        if (!isCurrentLoad(generation)) return;
        failureReported = true;
        clearPlaybackRequest();
        cancelPlaybackTasks();
        reportPlaying(false);
        if (rendererGone) released = true;
        if (listener != null) listener.onFailure(this, rendererGone);
    }

    private static boolean isHttpScheme(@Nullable String scheme) {
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
        if (released || browserListener == null) return;
        int generation = ++downloadGeneration;
        if (!isSameOriginDownload(url, getUrl())) {
            reportDownloadError(generation, null, "native-origin");
            return;
        }
        if (contentLength > MAX_DOWNLOAD_BYTES) {
            reportDownloadError(generation, null, "native-size");
            return;
        }
        String key = Integer.toString(generation);
        String script = BrowserScripts.DOWNLOAD_READ.replace("$KEY", JSONObject.quote(key)).replace("$URL", JSONObject.quote(url)).replace("$LIMIT", Integer.toString(MAX_DOWNLOAD_BYTES));
        String fileName = sanitizeFileName(URLUtil.guessFileName(url, contentDisposition, mimeType));
        long deadline = SystemClock.uptimeMillis() + DOWNLOAD_TIMEOUT;
        evaluateJavascript(script, value -> pollDownload(generation, key, fileName, mimeType, deadline));
    }

    private void installDownloadHook() {
        String script = BrowserScripts.DOWNLOAD_HOOK.replace("$BRIDGE", JSONObject.quote(DOWNLOAD_BRIDGE_NAME)).replace("$TOKEN", JSONObject.quote(downloadBridgeToken)).replace("$LIMIT", Integer.toString(MAX_DOWNLOAD_BYTES));
        evaluateJavascript(script, null);
    }

    private void onBridgeDownload(String token, String pageUrl, String blobUrl, String fileName, String mimeType, String encoded) {
        if (!isTrustedBridgeDownload(token, pageUrl, blobUrl) || encoded == null) {
            SpiderDebug.log(TAG, "download bridge rejected");
            return;
        }
        int generation = ++downloadGeneration;
        try {
            deliverDownload(fileName, mimeType, encoded);
        } catch (JSONException e) {
            reportDownloadError(generation, null, "bridge-" + e.getMessage());
        }
    }

    private void onBridgeDownloadError(String token, String pageUrl, String blobUrl) {
        if (!isTrustedBridgeDownload(token, pageUrl, blobUrl)) {
            SpiderDebug.log(TAG, "download bridge error rejected");
            return;
        }
        int generation = ++downloadGeneration;
        reportDownloadError(generation, null, "bridge-read");
    }

    private boolean isTrustedBridgeDownload(String token, String pageUrl, String blobUrl) {
        return !released && browserListener != null && downloadBridgeToken.equals(token) && isSameOriginBlob(blobUrl, pageUrl) && isSameHttpOrigin(pageUrl, getUrl());
    }

    private void pollDownload(int generation, String key, String fileName, String mimeType, long deadline) {
        if (released || generation != downloadGeneration || browserListener == null) return;
        String script = BrowserScripts.DOWNLOAD_POLL.replace("$KEY", JSONObject.quote(key));
        evaluateJavascript(script, value -> onDownloadPolled(generation, key, fileName, mimeType, deadline, value));
    }

    private void onDownloadPolled(int generation, String key, String fileName, String mimeType, long deadline, String value) {
        if (released || generation != downloadGeneration || browserListener == null) return;
        if (value == null || "null".equals(value)) {
            if (SystemClock.uptimeMillis() >= deadline) reportDownloadError(generation, key, "native-timeout");
            else postDelayed(() -> pollDownload(generation, key, fileName, mimeType, deadline), DOWNLOAD_POLL_DELAY);
            return;
        }
        completeDownload(generation, key, fileName, mimeType, value);
    }

    private void completeDownload(int generation, String key, String fileName, String reportedMimeType, String value) {
        try {
            String json = new JSONArray("[" + value + "]").getString(0);
            JSONObject result = new JSONObject(json);
            if (!"done".equals(result.optString("state"))) throw new JSONException("Download read failed");
            String mimeType = result.optString("type");
            if (mimeType.isEmpty()) mimeType = reportedMimeType;
            deliverDownload(fileName, mimeType, result.getString("data"));
        } catch (JSONException e) {
            reportDownloadError(generation, key, "native-" + e.getMessage());
        }
    }

    private void deliverDownload(String fileName, String mimeType, String encoded) throws JSONException {
        String normalized = normalizeDownloadMimeType(mimeType);
        if (normalized == null) throw new JSONException("Unsupported download type");
        byte[] data = decodeDownloadData(encoded);
        downloadGeneration++;
        browserListener.onDownload(new Download(sanitizeFileName(fileName), normalized, data));
    }

    static byte[] decodeDownloadData(@NonNull String encoded) throws JSONException {
        if (encoded.length() > MAX_DOWNLOAD_BASE64_CHARS) throw new JSONException("Download is too large");
        try {
            byte[] data = Base64.decode(encoded, Base64.DEFAULT);
            if (data.length > MAX_DOWNLOAD_BYTES) throw new JSONException("Download is too large");
            return data;
        } catch (IllegalArgumentException e) {
            throw new JSONException("Invalid download data");
        }
    }

    private void reportDownloadError(int generation, @Nullable String key, String reason) {
        if (generation != downloadGeneration || browserListener == null) return;
        downloadGeneration++;
        SpiderDebug.log(TAG, "download failed reason=%s", reason);
        if (key != null) {
            String script = "if (window.__fongmiDownloads) delete window.__fongmiDownloads[" + JSONObject.quote(key) + "]";
            evaluateJavascript(script, null);
        }
        browserListener.onDownloadError();
    }

    private static boolean isSameOriginBlob(@Nullable String blobUrl, @Nullable String pageUrl) {
        if (blobUrl == null || pageUrl == null || !blobUrl.startsWith("blob:")) return false;
        Uri blob = Uri.parse(blobUrl.substring(5));
        Uri page = Uri.parse(pageUrl);
        if (!isHttpScheme(blob.getScheme()) || !isHttpScheme(page.getScheme())) return false;
        return isSameHttpOrigin(blob, page);
    }

    static boolean isSameOriginDownload(@Nullable String downloadUrl, @Nullable String pageUrl) {
        if (downloadUrl == null) return false;
        return downloadUrl.startsWith("blob:") ? isSameOriginBlob(downloadUrl, pageUrl) : isSameHttpOrigin(downloadUrl, pageUrl);
    }

    static boolean isSameHttpOrigin(@Nullable String firstUrl, @Nullable String secondUrl) {
        if (firstUrl == null || secondUrl == null) return false;
        return isSameHttpOrigin(Uri.parse(firstUrl), Uri.parse(secondUrl));
    }

    private static boolean isSameHttpOrigin(@NonNull Uri first, @NonNull Uri second) {
        if (!isHttpScheme(first.getScheme()) || !isHttpScheme(second.getScheme())) return false;
        String host = first.getHost();
        return first.getScheme().equalsIgnoreCase(second.getScheme()) && host != null && host.equalsIgnoreCase(second.getHost()) && effectivePort(first) == effectivePort(second);
    }

    private static int effectivePort(@NonNull Uri uri) {
        if (uri.getPort() != -1) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    @Nullable
    static String normalizeDownloadMimeType(@Nullable String mimeType) {
        if (mimeType == null) return null;
        int parameter = mimeType.indexOf(';');
        String value = parameter < 0 ? mimeType : mimeType.substring(0, parameter);
        value = value.trim();
        if ("application/json".equalsIgnoreCase(value)) return "application/json";
        if ("text/plain".equalsIgnoreCase(value)) return "text/plain";
        return null;
    }

    static String sanitizeFileName(@Nullable String value) {
        if (value == null) return "config.json";
        String fileName = value.replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]", "_").trim();
        if (fileName.isEmpty()) return "config.json";
        return fileName.length() > 128 ? fileName.substring(0, 128) : fileName;
    }

    public void release() {
        if (released) return;
        hideCustomView();
        reportChallenge(false);
        clearPlaybackRequest();
        released = true;
        downloadGeneration++;
        cancelPlaybackTasks();
        if (browserListener != null) removeJavascriptInterface(DOWNLOAD_BRIDGE_NAME);
        stopLoading();
        loadUrl(BLANK);
        destroy();
    }

    private final class ChromeClient extends WebChromeClient {

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (browserListener == null) return false;
            browserListener.onShowFileChooser(callback, params);
            return true;
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            BrowserWebView.this.showCustomView(view, callback);
        }

        @Override
        public void onHideCustomView() {
            hideCustomView();
        }
    }

    private class Client extends WebViewClient {

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            boolean cloudflare = WebViewCloudflare.isResource(uri);
            if (WebViewCloudflare.isTurnstile(uri)) postChallenge();
            if (!cloudflare && WebViewRules.isAd(uri)) return WebViewRules.empty();
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        @SuppressLint("WebViewClientOnReceivedSslError")
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.cancel();
            if (isCurrentPlaybackPage(view, error.getUrl())) reportFailure(loadGeneration, false);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request.isForMainFrame() && isCurrentPlaybackPage(view, request.getUrl().toString())) reportFailure(loadGeneration, false);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            if (WebViewCloudflare.isChallenge(errorResponse)) {
                postChallenge();
                return;
            }
            if (request.isForMainFrame() && isCurrentPlaybackPage(view, request.getUrl().toString())) reportFailure(loadGeneration, false);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (isHttpScheme(request.getUrl().getScheme())) return false;
            if (browserListener != null && request.isForMainFrame() && request.hasGesture()) browserListener.onOpenExternal(request.getUrl());
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            if (isPlayback()) {
                if (!playbackActive) return;
                if (awaitingPlaybackStart) {
                    if (url != null && url.equals(previousPlaybackUrl) && !url.equals(requestedPlaybackUrl)) return;
                    awaitingPlaybackStart = false;
                    previousPlaybackUrl = null;
                    requestedPlaybackUrl = null;
                } else if (url == null || !url.equals(view.getUrl())) {
                    return;
                }
            }
            pageGeneration++;
            if (!isPlayback()) return;
            resetPlaybackState();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (BLANK.equals(url)) return;
            if (isPlayback() && !isCurrentPlaybackPage(view, url)) return;
            if (browserListener != null) installDownloadHook();
            int load = loadGeneration;
            int page = pageGeneration;
            if (browserListener != null) {
                postVisualStateCallback(page, new VisualStateCallback() {
                    @Override
                    public void onComplete(long requestId) {
                        if (isCurrentPage(load, page, url)) browserListener.onPageReady(BrowserWebView.this);
                    }
                });
            }
            evaluateJavascript(WebViewCloudflare.CHECK_SCRIPT, value -> onChallengeChecked(url, load, page, value));
        }

        private void onChallengeChecked(String url, int load, int page, String value) {
            if (!isCurrentPage(load, page, url)) return;
            if ("true".equalsIgnoreCase(value)) {
                showChallenge();
                return;
            }
            reportChallenge(false);
            WebViewRules.apply(BrowserWebView.this, Uri.parse(url), clickScript, () -> isCurrentPage(load, page, url));
            if (!isPlayback()) return;
            setAlpha(1f);
            if (playbackPaused) return;
            removeCallbacks(targetTask);
            post(targetTask);
        }
    }

    private final class DownloadBridge {

        @JavascriptInterface
        public void onDownload(String token, String pageUrl, String blobUrl, String fileName, String mimeType, String encoded) {
            post(() -> onBridgeDownload(token, pageUrl, blobUrl, fileName, mimeType, encoded));
        }

        @JavascriptInterface
        public void onError(String token, String pageUrl, String blobUrl) {
            post(() -> onBridgeDownloadError(token, pageUrl, blobUrl));
        }
    }

    void showChallenge() {
        if (released) return;
        if (!challengeVisible) {
            SpiderDebug.log(TAG, "cloudflare=visible");
            reportChallenge(true);
        }
        setAlpha(1f);
        requestFocus();
    }

    private void reportChallenge(boolean visible) {
        if (challengeVisible == visible) return;
        challengeVisible = visible;
        if (listener != null) listener.onChallengeChanged(this, visible);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private final class OreoClient extends Client {

        @Override
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            hideCustomView();
            if (isPlayback()) {
                if (!released) reportFailure(loadGeneration, true);
            } else if (!released) {
                released = true;
                downloadGeneration++;
                if (browserListener != null) browserListener.onRendererGone(BrowserWebView.this);
            }
            destroy();
            return true;
        }
    }
}
