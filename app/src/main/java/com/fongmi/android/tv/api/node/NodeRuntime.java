package com.fongmi.android.tv.api.node;

import androidx.annotation.MainThread;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.ui.activity.WebActivity;
import com.fongmi.android.tv.ui.custom.WebViewCookies;
import com.fongmi.android.tv.utils.Notify;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.runtime.NodeRuntimeApi;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.RuntimeModule;
import com.github.catvod.utils.Task;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class NodeRuntime implements NodeRuntimeApi.Callback {

    private static final String SUCCESS_RESPONSE = "{\"success\":true,\"code\":0}";
    private static final String FAILURE_RESPONSE = "{\"success\":false,\"code\":-1}";

    private final NodePlayback playback = new NodePlayback();
    private volatile NodeRuntimeApi.Client client;

    private NodeRuntime() {
    }

    public static NodeRuntime get() {
        return Loader.INSTANCE;
    }

    public static boolean isConfig(String url) {
        if (url == null || url.regionMatches(true, 0, "content://", 0, 10)) return false;
        String value = url.split("[?#]", 2)[0];
        try {
            value = URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
        }
        return value.toLowerCase(Locale.ROOT).endsWith("/index.js.md5") || value.equalsIgnoreCase("index.js.md5");
    }

    public synchronized NodeRuntimeApi.Client client() {
        if (client == null) client = RuntimeModule.NODEJS.entry(NodeRuntimeApi.class).create(App.get(), this);
        return client;
    }

    public NodeRuntimeApi.LoadedConfig loadConfig(String source) throws Exception {
        playback.clear();
        return client().loadConfig(source);
    }

    public void cancelLoad() {
        if (client != null) client.cancelLoad();
    }

    public void accept(NodeRuntimeApi.LoadedConfig loaded) throws IOException {
        client().accept(loaded);
    }

    public void prune(NodeRuntimeApi.LoadedConfig loaded, Iterable<String> sources, String currentSource) throws IOException {
        if (client != null) client.prune(loaded, sources, currentSource);
    }

    public void fail(NodeRuntimeApi.LoadedConfig loaded) {
        if (client != null && client.fail(loaded)) playback.clear();
    }

    public void clear() {
        if (client != null) client.clear();
        playback.clear();
    }

    @MainThread
    public void releaseIfUnused() {
        if (HomeActivity.isRunning() || PlaybackService.isRunning()) return;
        VodConfig.get().cancel();
        Task.execute(this::clearIfUnused);
    }

    private synchronized void clearIfUnused() {
        if (HomeActivity.isRunning() || PlaybackService.isRunning()) return;
        clear();
    }

    @Override
    public void onPlayInfo(String flag, String id) {
        playback.setPlayInfo(flag, id);
    }

    @Override
    public String onMessage(String action, JsonObject options) {
        try {
            return switch (action) {
                case "toast" -> onToast(options);
                case "getPlayInfo" -> playback.getPlayInfo();
                case "danmuPush" -> playback.pushDanmaku(Json.safeString(options, "url")) ? SUCCESS_RESPONSE : FAILURE_RESPONSE;
                case "clearDanmu" -> playback.clearDanmaku() ? SUCCESS_RESPONSE : FAILURE_RESPONSE;
                case "openWeb", "openInternalWebview" -> WebActivity.openWeb(options.toString()) ? SUCCESS_RESPONSE : FAILURE_RESPONSE;
                case "getCookie" -> cookie(options);
                case "setCookie" -> WebViewCookies.set(Json.safeString(options, "url"), Json.safeString(options, "cookie")) ? SUCCESS_RESPONSE : FAILURE_RESPONSE;
                case "getUserAgent" -> userAgent();
                default -> FAILURE_RESPONSE;
            };
        } catch (Exception e) {
            SpiderDebug.log(e);
            return FAILURE_RESPONSE;
        }
    }

    private String onToast(JsonObject options) {
        String message = Json.safeString(options, "message");
        if (!message.isEmpty()) App.post(() -> Notify.show(message));
        return SUCCESS_RESPONSE;
    }

    private String cookie(JsonObject options) {
        JsonObject result = JsonParser.parseString(SUCCESS_RESPONSE).getAsJsonObject();
        result.addProperty("cookie", WebViewCookies.get(Json.safeString(options, "url")));
        return result.toString();
    }

    private String userAgent() {
        JsonObject result = JsonParser.parseString(SUCCESS_RESPONSE).getAsJsonObject();
        result.addProperty("ua", WebActivity.getUserAgent());
        return result.toString();
    }

    private static class Loader {
        private static final NodeRuntime INSTANCE = new NodeRuntime();
    }
}
