package com.fongmi.android.tv.api.node;

import android.net.Uri;
import android.os.Looper;
import android.text.TextUtils;

import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.playback.vod.VodPlaybackHost;
import com.fongmi.android.tv.playback.vod.VodPlaybackInfo;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.PlaybackService;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class NodeRuntime {

    private static final String SUCCESS_RESPONSE = "{\"success\":true,\"code\":0}";
    private static final String FAILURE_RESPONSE = "{\"success\":false,\"code\":-1}";

    private final Object profileLock = new Object();
    private final NodeClient client;
    private volatile PlayInfo playInfo = PlayInfo.empty();

    private NodeRuntime() {
        client = new NodeClient(App.get(), new NodeClient.Callback() {
            @Override
            public String onMessage(String action, JsonObject options) {
                return NodeRuntime.this.onMessage(action, options);
            }

            @Override
            public void onPlayInfo(String flag, String id) {
                setPlayInfo(flag, id);
            }
        });
    }

    public static NodeRuntime get() {
        return Loader.INSTANCE;
    }

    public NodeClient client() {
        return client;
    }

    public NodeClient.LoadedConfig loadConfig(String source) throws Exception {
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
        if (client.fail(loaded)) playInfo = PlayInfo.empty();
    }

    public void clear() {
        client.clear();
        playInfo = PlayInfo.empty();
    }

    public void setPlayInfo(String flag, String id) {
        VodPlaybackInfo.Snapshot snapshot = VodPlaybackInfo.get();
        String title = snapshot.title();
        String episodeName = snapshot.episodeName();
        if (TextUtils.isEmpty(flag)) flag = snapshot.flag();
        if (title.isEmpty() && App.activity() instanceof VodPlaybackHost host) {
            title = host.getVodName();
            episodeName = host.getVodMark();
        }
        playInfo = new PlayInfo(text(title), text(episodeName), fileName(id), text(flag), text(id));
    }

    private String onMessage(String action, JsonObject options) {
        try {
            return switch (action) {
                case "toast" -> onToast(options);
                case "getPlayInfo" -> getPlayInfo();
                case "danmuPush" -> pushDanmaku(options);
                case "clearDanmu" -> clearDanmaku();
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

    private String getPlayInfo() {
        PlaybackService service = Server.get().getService();
        PlayerManager player = service == null ? null : service.player();
        MediaMetadata metadata = player == null ? null : player.getMetadata();
        PlayInfo pending = playInfo;
        String title = text(metadata == null ? null : metadata.title);
        String episodeName = text(metadata == null ? null : metadata.artist);
        if (title.isEmpty()) title = pending.title();
        if (episodeName.isEmpty()) episodeName = pending.episodeName();
        VodPlaybackInfo.Snapshot snapshot = VodPlaybackInfo.get();
        if (title.isEmpty()) title = snapshot.title();
        if (episodeName.isEmpty()) episodeName = snapshot.episodeName();
        if (App.activity() instanceof VodPlaybackHost host) {
            if (title.isEmpty()) title = host.getVodName();
            if (episodeName.isEmpty()) episodeName = host.getVodMark();
        }
        String url = player == null ? "" : text(player.getUrl());
        String fileName = pending.fileName().isEmpty() ? fileName(url) : pending.fileName();
        long[] times = getPlaybackTimes(player);
        JsonObject result = new JsonObject();
        result.addProperty("title", title);
        result.addProperty("episodeName", episodeName);
        result.addProperty("fileName", fileName);
        result.addProperty("flag", pending.flag());
        result.addProperty("id", pending.id());
        result.addProperty("url", url);
        result.addProperty("key", player == null ? "" : text(player.getKey()));
        result.addProperty("position", times[0]);
        result.addProperty("duration", times[1]);
        SpiderDebug.log("NodeRuntime", "getPlayInfo title=%s, episode=%s, flag=%s", title, episodeName, pending.flag());
        return result.toString();
    }

    private String openInternalWebView(JsonObject options) {
        String url = NodeClient.normalizeInternalUrl(Json.safeString(options, "url"), client.getAddress());
        return WebActivity.open(url) ? SUCCESS_RESPONSE : FAILURE_RESPONSE;
    }

    private static long[] getPlaybackTimes(PlayerManager player) {
        long[] result = new long[2];
        if (player == null) return result;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            readPlaybackTimes(player, result);
            return result;
        }
        CountDownLatch latch = new CountDownLatch(1);
        App.post(() -> {
            readPlaybackTimes(player, result);
            latch.countDown();
        });
        try {
            latch.await(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return result;
    }

    private static void readPlaybackTimes(PlayerManager player, long[] result) {
        try {
            result[0] = player.getPosition();
            result[1] = player.getDuration();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
    }

    private String pushDanmaku(JsonObject options) {
        String url = Json.safeString(options, "url");
        if (url.isEmpty()) return clearDanmaku();
        App.post(() -> RefreshEvent.danmaku(url));
        return SUCCESS_RESPONSE;
    }

    private String clearDanmaku() {
        PlaybackService service = Server.get().getService();
        if (service == null || service.player() == null) return FAILURE_RESPONSE;
        App.post(() -> service.player().clearDanmaku());
        return SUCCESS_RESPONSE;
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

    private static String fileName(String value) {
        if (TextUtils.isEmpty(value)) return "";
        try {
            String name = Uri.parse(value).getLastPathSegment();
            return TextUtils.isEmpty(name) ? value : name;
        } catch (Exception e) {
            return value;
        }
    }

    private static String text(CharSequence value) {
        return value == null ? "" : value.toString();
    }

    private record PlayInfo(String title, String episodeName, String fileName, String flag, String id) {
        private static PlayInfo empty() {
            return new PlayInfo("", "", "", "", "");
        }
    }

    private static class Loader {
        private static final NodeRuntime INSTANCE = new NodeRuntime();
    }
}
