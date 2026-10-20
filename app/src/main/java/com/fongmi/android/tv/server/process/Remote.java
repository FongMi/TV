package com.fongmi.android.tv.server.process;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Window;

import androidx.activity.ComponentActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.bean.Danmaku;
import com.fongmi.android.tv.bean.Sub;
import com.fongmi.android.tv.player.PlaybackLog;
import com.fongmi.android.tv.player.PlaybackSnapshot;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.media.ArtworkBitmapLoader;
import com.fongmi.android.tv.player.media.MediaItemFactory;
import com.fongmi.android.tv.server.LogSession;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.server.impl.Process;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.SpeedSetting;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Crypto;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import okhttp3.HttpUrl;

public class Remote implements Process {

    private static final Set<String> COMMANDS = Set.of("play", "pause", "stop", "prev", "next", "repeat_on", "repeat_off", "seek", "speed");
    private static final Set<String> KEY_ACTIONS = Set.of("press", "down", "repeat", "up", "cancel");
    private static final int KEY_RELEASE_TIMEOUT_MS = 5000;

    private static final Map<String, Integer> KEYS = Map.ofEntries(
            Map.entry("up", KeyEvent.KEYCODE_DPAD_UP), Map.entry("down", KeyEvent.KEYCODE_DPAD_DOWN),
            Map.entry("left", KeyEvent.KEYCODE_DPAD_LEFT), Map.entry("right", KeyEvent.KEYCODE_DPAD_RIGHT),
            Map.entry("ok", KeyEvent.KEYCODE_DPAD_CENTER), Map.entry("back", KeyEvent.KEYCODE_BACK), Map.entry("menu", KeyEvent.KEYCODE_MENU),
            Map.entry("0", KeyEvent.KEYCODE_0), Map.entry("1", KeyEvent.KEYCODE_1), Map.entry("2", KeyEvent.KEYCODE_2),
            Map.entry("3", KeyEvent.KEYCODE_3), Map.entry("4", KeyEvent.KEYCODE_4), Map.entry("5", KeyEvent.KEYCODE_5),
            Map.entry("6", KeyEvent.KEYCODE_6), Map.entry("7", KeyEvent.KEYCODE_7), Map.entry("8", KeyEvent.KEYCODE_8), Map.entry("9", KeyEvent.KEYCODE_9));

    private KeyPress pressedKey;

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return url.equals("/remote") || url.equals("/remote/key") || url.equals("/remote/artwork") || url.equals("/remote/info") || url.equals("/remote/subtitle") || url.equals("/remote/danmaku") || url.equals("/log/info");
    }

    @Override
    public Response doResponse(IHTTPSession session, String url, Map<String, String> files) {
        boolean key = session.getMethod() == Method.POST && url.equals("/remote/key");
        boolean control = session.getMethod() == Method.POST && url.equals("/remote");
        boolean subtitle = session.getMethod() == Method.POST && url.equals("/remote/subtitle");
        boolean danmaku = session.getMethod() == Method.POST && url.equals("/remote/danmaku");
        boolean media = subtitle || danmaku;
        if (url.equals("/remote/key") && !key || url.equals("/remote/subtitle") && !subtitle || url.equals("/remote/danmaku") && !danmaku || session.getMethod() != Method.GET && !control && !media && !key) return response(Status.METHOD_NOT_ALLOWED, "GET or POST required");
        if (url.equals("/remote/info")) return response(Status.OK, deviceInfo().toString());
        if (url.equals("/remote/artwork")) return artwork(session.getParms().get("key"));
        String type = session.getParms().get("type");
        if (control && !COMMANDS.contains(type == null ? "" : type)) return response(Status.BAD_REQUEST, "Invalid control");
        Integer keyCode = key ? KEYS.get(session.getParms().getOrDefault("key", "")) : null;
        if (key && keyCode == null) return response(Status.BAD_REQUEST, "Invalid remote key");
        String keyAction = session.getParms().getOrDefault("action", "press");
        String keyId = session.getParms().getOrDefault("id", "");
        if (key && (!KEY_ACTIONS.contains(keyAction) || !keyAction.equals("press") && !keyId.matches("[a-zA-Z0-9-]{1,64}"))) return response(Status.BAD_REQUEST, "Invalid remote key action");
        if ((control || media || key) && !isSameOrigin(session.getHeaders())) return response(Status.FORBIDDEN, "Same origin required");
        long position = control && type.equals("seek") ? seekPosition(session.getParms().get("position")) : 0;
        if (position < 0) return response(Status.BAD_REQUEST, "Invalid seek position");
        float speed = control && type.equals("speed") ? speedValue(session.getParms().get("speed")) : 1;
        if (!Float.isFinite(speed) || speed < SpeedSetting.MIN || speed > SpeedSetting.MAX) return response(Status.BAD_REQUEST, "Invalid playback speed");
        String mediaKey = session.getParms().get("key");
        if (media && (mediaKey == null || !mediaKey.matches("[0-9a-f]{32}"))) return response(Status.BAD_REQUEST, "Invalid playback key");
        Sub sub;
        Danmaku comments;
        try {
            sub = subtitle ? subtitle(session.getParms().get("path")) : null;
            comments = danmaku ? danmaku(session.getParms().get("path")) : null;
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            return response(Status.BAD_REQUEST, "Invalid media path");
        }
        FutureTask<Response> task = new FutureTask<>(() -> {
            if (key) {
                if (!dispatchKey(keyCode, keyAction, keyId)) return response(Status.CONFLICT, "Remote key unavailable");
                boolean holding = pressedKey != null && pressedKey.id.equals(keyId);
                return response(Status.OK, "{\"accepted\":true,\"holding\":" + holding + "}");
            }
            PlaybackService service = Server.get().getService();
            if (control && (service == null || !service.dispatchRemoteControl(type, position, speed))) return response(Status.CONFLICT, "Playback unavailable");
            if (media && (service == null || !mediaKey.equals(Crypto.md5(PlaybackSnapshot.capture(service.player()).url())))) return response(Status.CONFLICT, "Playback unavailable");
            if (subtitle && !service.dispatchRemoteSubtitle(sub) || danmaku && !service.dispatchRemoteDanmaku(comments)) return response(Status.CONFLICT, "Playback unavailable");
            JsonObject body = playback(service, url.equals("/log/info"));
            if (url.equals("/log/info")) body = buildLogInfo(service, body);
            return response(Status.OK, body.toString());
        });
        return awaitPlayback(task);
    }

    private Response awaitPlayback(FutureTask<Response> task) {
        App.post(task);
        try {
            return task.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return response(Status.SERVICE_UNAVAILABLE, "Request interrupted");
        } catch (TimeoutException e) {
            return response(Status.SERVICE_UNAVAILABLE, "Playback busy");
        } catch (ExecutionException e) {
            SpiderDebug.log(e.getCause());
            return response(Status.INTERNAL_ERROR, "Playback request failed");
        } finally {
            task.cancel(false);
            App.removeCallbacks(task);
        }
    }

    private Window.Callback keyTarget() {
        Activity activity = App.activity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return null;
        return activity.hasWindowFocus() ? activity : activity instanceof FragmentActivity owner ? dialogTarget(owner.getSupportFragmentManager()) : null;
    }

    private boolean dispatchKey(int keyCode, String action, String id) {
        KeyPress press = pressedKey;
        if (action.equals("up") || action.equals("cancel")) {
            if (press != null && press.keyCode == keyCode && press.id.equals(id)) press.finish(action.equals("cancel") || keyTarget() != press.target);
            return true;
        }
        if (action.equals("repeat")) {
            if (press == null || press.keyCode != keyCode || !press.id.equals(id)) return false;
            if (keyTarget() != press.target) {
                press.finish(true);
                return true;
            }
            press.send(KeyEvent.ACTION_DOWN, ++press.repeat == 1 ? KeyEvent.FLAG_LONG_PRESS : 0);
            App.post(press.expire, KEY_RELEASE_TIMEOUT_MS);
            return true;
        }
        if (press != null) return false;
        Window.Callback target = keyTarget();
        if (target == null) return false;
        press = new KeyPress(target, keyCode, id);
        pressedKey = press;
        press.send(KeyEvent.ACTION_DOWN, 0);
        if (action.equals("press")) press.finish(false);
        else App.post(press.expire, KEY_RELEASE_TIMEOUT_MS);
        return true;
    }

    private final class KeyPress {

        private final Window.Callback target;
        private final int keyCode;
        private final String id;
        private final long downTime = SystemClock.uptimeMillis();
        private final Runnable expire = () -> finish(true);
        private int repeat;

        private KeyPress(Window.Callback target, int keyCode, String id) {
            this.target = target;
            this.keyCode = keyCode;
            this.id = id;
        }

        private void send(int action, int flags) {
            if (keyCode == KeyEvent.KEYCODE_BACK && target instanceof ComponentActivity owner) {
                if (action == KeyEvent.ACTION_UP && flags == 0) owner.getOnBackPressedDispatcher().onBackPressed();
            } else {
                target.dispatchKeyEvent(new KeyEvent(downTime, SystemClock.uptimeMillis(), action, keyCode, action == KeyEvent.ACTION_DOWN ? repeat : 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, flags));
            }
        }

        private void finish(boolean cancelled) {
            App.removeCallbacks(expire);
            if (pressedKey != this) return;
            pressedKey = null;
            send(KeyEvent.ACTION_UP, cancelled ? KeyEvent.FLAG_CANCELED : 0);
        }
    }

    private Window.Callback dialogTarget(FragmentManager manager) {
        for (Fragment fragment : manager.getFragments()) {
            if (!fragment.isAdded()) continue;
            if (fragment instanceof DialogFragment owner) {
                Dialog dialog = owner.getDialog();
                if (dialog != null && dialog.getWindow() != null && dialog.getWindow().getDecorView().hasWindowFocus()) return dialog;
            }
            Window.Callback target = dialogTarget(fragment.getChildFragmentManager());
            if (target != null) return target;
        }
        return null;
    }

    private JsonObject buildLogInfo(PlaybackService service, JsonObject playback) {
        playback.remove("artwork");
        JsonObject info = deviceInfo();
        info.add("playback", playback);
        JsonObject collection = LogSession.get().info();
        collection.addProperty("readLogsPermission", App.get().checkSelfPermission(Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED);
        collection.addProperty("uid", android.os.Process.myUid());
        collection.addProperty("pid", android.os.Process.myPid());
        info.add("collection", collection);
        info.add("playbackLog", PlaybackLog.capture(service == null ? null : service.player()));
        info.add("lastPlaybackError", PlaybackLog.getLastError());
        return info;
    }

    private JsonObject deviceInfo() {
        JsonObject info = new JsonObject();
        info.addProperty("version", BuildConfig.VERSION_NAME);
        info.addProperty("versionCode", BuildConfig.VERSION_CODE);
        info.addProperty("variant", BuildConfig.FLAVOR);
        info.addProperty("buildType", BuildConfig.BUILD_TYPE);
        info.addProperty("device", Util.getDeviceName());
        info.addProperty("android", Build.VERSION.RELEASE);
        info.addProperty("sdk", Build.VERSION.SDK_INT);
        info.addProperty("abis", String.join(", ", Build.SUPPORTED_ABIS));
        return info;
    }

    private JsonObject playback(PlaybackService service, boolean logInfo) {
        JsonObject body = new JsonObject();
        boolean active = service != null && service.hasRemotePlayback();
        body.addProperty("active", active);
        if (!active && !logInfo) return body;
        PlayerManager player = service == null ? null : service.player();
        PlaybackSnapshot snapshot = PlaybackSnapshot.capture(player);
        if (logInfo) PlaybackLog.addUrl(body, snapshot.url());
        if (!active) return body;
        MediaMetadata metadata = snapshot.playingMetadata();
        body.addProperty("title", MediaItemFactory.getDisplayTitle(metadata));
        body.addProperty("subtitle", metadata.subtitle == null ? "" : metadata.subtitle.toString());
        body.addProperty("mediaKey", Crypto.md5(snapshot.url()));
        body.addProperty("artwork", metadata.artworkUri == null ? "" : "/remote/artwork?key=" + Crypto.md5(metadata.artworkUri.toString()));
        body.addProperty("position", Math.max(0, snapshot.position()));
        body.addProperty("duration", Math.max(0, snapshot.duration()));
        body.addProperty("state", snapshot.state());
        body.addProperty("playWhenReady", player.getPlaybackState() != Player.STATE_ENDED && player.getPlayer().getPlayWhenReady());
        body.addProperty("repeatOne", player.isRepeatOne());
        body.addProperty("live", metadata.mediaType != null && metadata.mediaType == MediaMetadata.MEDIA_TYPE_TV_CHANNEL || player.isLive());
        body.addProperty("canNavigate", service.canRemoteNavigate());
        body.addProperty("canSeek", service.canRemoteSeek());
        body.addProperty("canSetSpeed", service.canRemoteSetSpeed());
        body.addProperty("speed", player.getSpeed());
        body.addProperty("engine", player.getEngine() == PlayerSetting.ENGINE_MPV ? "mpv" : "ExoPlayer");
        body.addProperty("video", player.getSizeText());
        return body;
    }

    private Response artwork(String key) {
        if (key == null || !key.matches("[0-9a-f]{32}")) return response(Status.BAD_REQUEST, "Invalid artwork key");
        FutureTask<Uri> source = new FutureTask<>(() -> {
            PlaybackService service = Server.get().getService();
            if (service == null || !service.hasRemotePlayback()) return null;
            MediaItem item = service.player().getCurrentMediaItem();
            Uri uri = item == null ? null : item.mediaMetadata.artworkUri;
            return uri != null && key.equals(Crypto.md5(uri.toString())) ? uri : null;
        });
        App.post(source);
        ListenableFuture<Bitmap> loading = null;
        try {
            Uri uri = source.get(2, TimeUnit.SECONDS);
            if (uri == null) return response(Status.NOT_FOUND, "Artwork unavailable");
            loading = new ArtworkBitmapLoader(App.get()).loadBitmap(uri);
            Bitmap bitmap = loading.get(5, TimeUnit.SECONDS);
            return encodeArtwork(bitmap);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return response(Status.SERVICE_UNAVAILABLE, "Request interrupted");
        } catch (TimeoutException e) {
            return response(Status.SERVICE_UNAVAILABLE, "Artwork busy");
        } catch (ExecutionException e) {
            SpiderDebug.log(e.getCause());
            return response(Status.INTERNAL_ERROR, "Artwork request failed");
        } finally {
            if (loading != null) loading.cancel(true);
            source.cancel(false);
            App.removeCallbacks(source);
        }
    }

    private Response encodeArtwork(Bitmap bitmap) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) return response(Status.INTERNAL_ERROR, "Artwork encoding failed");
        byte[] data = output.toByteArray();
        Response response = newFixedLengthResponse(Status.OK, "image/png", new ByteArrayInputStream(data), data.length);
        response.addHeader("Cache-Control", "private, max-age=60");
        return response;
    }

    private long seekPosition(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private float speedValue(String value) {
        try {
            return Float.parseFloat(value == null ? "" : value);
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    private Sub subtitle(String path) throws IOException {
        File file = Local.resolvePath(path);
        if (!file.isFile() || !file.getName().toLowerCase(Locale.ROOT).matches(".*\\.(srt|ssa|ass)$")) throw new IllegalArgumentException("Invalid subtitle file");
        return Sub.from(file.getName(), file.toURI().toString());
    }

    private Danmaku danmaku(String path) throws IOException {
        if (path == null || path.isBlank()) throw new IllegalArgumentException("Empty danmaku path");
        HttpUrl url = HttpUrl.parse(path);
        if (url != null) return Danmaku.from(url.toString());
        if (!path.startsWith("/")) throw new IllegalArgumentException("Invalid danmaku URL");
        File file = Local.resolvePath(path);
        if (!file.isFile()) throw new IllegalArgumentException("Invalid danmaku file");
        return Danmaku.from(file.getName(), file.toURI().toString());
    }

    private boolean isSameOrigin(Map<String, String> headers) {
        String origin = headers.get("origin");
        if (origin == null) return true;
        HttpUrl source = HttpUrl.parse(origin);
        String host = headers.get("host");
        HttpUrl target = host == null ? null : HttpUrl.parse("http://" + host);
        return source != null && source.equals(target);
    }

    private Response response(Status status, String text) {
        Response response = newFixedLengthResponse(status, status == Status.OK ? "application/json; charset=utf-8" : "text/plain; charset=utf-8", text);
        response.addHeader("Cache-Control", "no-store");
        return response;
    }
}
