package com.fongmi.android.tv.api.node;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.ui.activity.WebActivity;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.nodejs.NodeClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Json;
import com.github.catvod.utils.Path;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class NodeRuntime implements NodeClient.Callback {

    private static final String SUCCESS_RESPONSE = "{\"success\":true,\"code\":0}";
    private static final String FAILURE_RESPONSE = "{\"success\":false,\"code\":-1}";

    private final Object profileLock = new Object();
    private final NodePlayback playback = new NodePlayback();
    private final NodeClient client;

    private NodeRuntime() {
        client = new NodeClient(App.get(), this);
    }

    public static NodeRuntime get() {
        return Loader.INSTANCE;
    }

    public NodeClient client() {
        return client;
    }

    public NodeClient.LoadedConfig loadConfig(String source) throws Exception {
        playback.clear();
        return client.loadConfig(source);
    }

    public void cancelLoad() {
        client.cancelLoad();
    }

    public void accept(NodeClient.LoadedConfig loaded) throws IOException {
        client.accept(loaded);
    }

    public void prune(NodeClient.LoadedConfig loaded, Iterable<String> sources, String currentSource) throws IOException {
        client.prune(loaded, sources, currentSource);
    }

    public void fail(NodeClient.LoadedConfig loaded) {
        if (client.fail(loaded)) playback.clear();
    }

    public void clear() {
        client.clear();
        playback.clear();
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
                case "queryProfile" -> readProfile().toString();
                case "saveProfile" -> saveProfile(options);
                case "openInternalWebview" -> openInternalWebView(options);
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

    private String openInternalWebView(JsonObject options) {
        String url = NodeClient.normalizeInternalUrl(Json.safeString(options, "url"), client.getAddress());
        return WebActivity.open(url) ? SUCCESS_RESPONSE : FAILURE_RESPONSE;
    }

    private JsonElement readProfile() {
        synchronized (profileLock) {
            File file = client.getProfileFile();
            if (file == null || !file.isFile()) return new JsonObject();
            try {
                return JsonParser.parseString(Path.read(file));
            } catch (Exception e) {
                return new JsonObject();
            }
        }
    }

    private String saveProfile(JsonObject profile) {
        synchronized (profileLock) {
            File file = client.getProfileFile();
            if (file == null) return FAILURE_RESPONSE;
            try {
                FileUtil.writeAtomically(profile.toString().getBytes(StandardCharsets.UTF_8), file);
                return SUCCESS_RESPONSE;
            } catch (IOException e) {
                SpiderDebug.log(e);
                return FAILURE_RESPONSE;
            }
        }
    }

    private static class Loader {
        private static final NodeRuntime INSTANCE = new NodeRuntime();
    }
}
