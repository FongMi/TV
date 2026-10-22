package com.fongmi.android.tv.player;

import android.net.Uri;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.media3.ui.danmaku.Danmaku;

import com.fongmi.android.tv.api.SiteApi;
import com.github.catvod.crawler.SpiderDebug;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class LiveDanmaku implements AutoCloseable {

    private final Consumer<Danmaku> emit;
    private final BiConsumer<String, String> status;

    private volatile long generation;
    private volatile boolean closed;
    private boolean enabled = true;
    private Uri source;
    private ExecutorService executor;
    private AutoCloseable subscription;
    private String state = "";
    private String msg = "";

    public LiveDanmaku(Consumer<Danmaku> emit, BiConsumer<String, String> status) {
        this.emit = emit;
        this.status = status;
    }

    public static boolean isLive(@Nullable Uri uri) {
        return uri != null && "spider".equalsIgnoreCase(uri.getScheme());
    }

    @MainThread
    public void setDataSource(@Nullable Uri uri) {
        if (closed || Objects.equals(source, uri)) return;
        source = uri;
        restart();
    }

    @MainThread
    public void setEnabled(boolean enabled) {
        if (closed || this.enabled == enabled) return;
        this.enabled = enabled;
        if (source != null) restart();
    }

    private synchronized void restart() {
        long token = ++generation;
        Uri uri = enabled ? source : null;
        updateState(uri == null ? "" : "connecting", "");
        if (executor == null) executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> subscribe(token, uri));
    }

    private boolean isCurrent(long token) {
        return !closed && generation == token;
    }

    private void subscribe(long token, @Nullable Uri uri) {
        if (!isCurrent(token)) return;
        closeSubscription();
        if (uri == null) return;
        try {
            subscription = SiteApi.danmaku(uri, item -> emit(token, item));
            if (!isCurrent(token)) closeSubscription();
        } catch (Exception error) {
            SpiderDebug.log(error);
            fail(token, error);
        }
    }

    private synchronized void emit(long token, JSONObject item) {
        if (!isCurrent(token)) return;
        if (item.has("state")) {
            String state = parseState(item);
            String msg = parseMessage(item);
            if (state.equals("error")) finish(msg);
            else updateState(state, msg);
        } else {
            emit.accept(parseItem(item));
        }
    }

    private synchronized void fail(long token, Exception error) {
        if (!isCurrent(token)) return;
        String msg = error.getMessage();
        if (msg == null || msg.isBlank()) msg = error.getClass().getSimpleName();
        finish(msg.substring(0, Math.min(msg.length(), 512)));
    }

    private void finish(String msg) {
        generation++;
        executor.execute(this::closeSubscription);
        updateState("error", msg);
    }

    private void updateState(String state, String msg) {
        if (this.state.equals(state) && this.msg.equals(msg)) return;
        this.state = state;
        this.msg = msg;
        status.accept(state, msg);
    }

    private static String parseState(JSONObject item) {
        if (item.has("text") || !(item.opt("state") instanceof String state) || !(state.equals("ready") || state.equals("retrying") || state.equals("error"))) throw new IllegalArgumentException("Invalid danmaku state");
        return state;
    }

    private static String parseMessage(JSONObject item) {
        if (!item.has("msg")) return "";
        if (!(item.opt("msg") instanceof String msg) || msg.length() > 512) throw new IllegalArgumentException("Invalid danmaku message");
        return msg;
    }

    static Danmaku parseItem(JSONObject item) {
        String text = parseText(item);
        int type = parseType(item);
        float size = parseSize(item);
        int color = toArgb(number(item, "color", 0xffffff));
        return new Danmaku(text, 0, type, color, size, colors(item));
    }

    private static String parseText(JSONObject item) {
        if (!(item.opt("text") instanceof String text) || text.isBlank() || text.length() > 4096) throw new IllegalArgumentException("Danmaku requires text up to 4096 characters");
        return text;
    }

    private static int parseType(JSONObject item) {
        double value = number(item, "type", Danmaku.TYPE_SCROLL);
        int type = (int) value;
        if (value != type || !isSupportedType(type)) throw new IllegalArgumentException("Invalid danmaku type");
        return type;
    }

    private static boolean isSupportedType(int type) {
        return type == Danmaku.TYPE_SCROLL || type == Danmaku.TYPE_BOTTOM || type == Danmaku.TYPE_TOP || type == Danmaku.TYPE_REVERSE;
    }

    private static float parseSize(JSONObject item) {
        double size = number(item, "size", 0);
        if (size < 0 || size > 96) throw new IllegalArgumentException("Invalid danmaku size");
        return (float) size;
    }

    private static double number(JSONObject item, String key, double defaultValue) {
        if (!item.has(key)) return defaultValue;
        if (!(item.opt(key) instanceof Number value) || !Double.isFinite(value.doubleValue())) throw new IllegalArgumentException("Invalid danmaku " + key);
        return value.doubleValue();
    }

    private static int toArgb(double value) {
        if (value < 0 || value > 0xffffff || value != (int) value) throw new IllegalArgumentException("Invalid danmaku color");
        return 0xff000000 | (int) value;
    }

    @Nullable
    private static int[] colors(JSONObject item) {
        if (!item.has("colors")) return null;
        if (!(item.opt("colors") instanceof JSONArray values) || values.length() < 2 || values.length() > 8) throw new IllegalArgumentException("Danmaku colors requires 2 to 8 RGB values");
        int[] colors = new int[values.length()];
        for (int index = 0; index < colors.length; index++) {
            if (!(values.opt(index) instanceof Number value)) throw new IllegalArgumentException("Invalid danmaku color");
            colors[index] = toArgb(value.doubleValue());
        }
        return colors;
    }

    private void closeSubscription() {
        AutoCloseable previous = subscription;
        subscription = null;
        if (previous == null) return;
        try {
            previous.close();
        } catch (Exception error) {
            SpiderDebug.log(error);
        }
    }

    @Override
    @MainThread
    public void close() {
        if (closed) return;
        synchronized (this) {
            closed = true;
            generation++;
            updateState("", "");
        }
        if (executor == null) return;
        executor.execute(this::closeSubscription);
        executor.shutdown();
    }
}
