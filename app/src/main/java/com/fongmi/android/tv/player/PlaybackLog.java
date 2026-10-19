package com.fongmi.android.tv.player;

import android.app.ActivityManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.DecoderMode;
import androidx.media3.common.Format;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.audio.AudioSink;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.setting.DecodeSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class PlaybackLog {

    private static final int MAX_URL_LENGTH = 16 * 1024;
    private static final int MAX_CAUSES = 8;
    private static final int MAX_TEXT_LENGTH = 1024;

    private static volatile JsonObject lastError;

    public static JsonObject capture(PlayerManager manager) {
        JsonObject body = new JsonObject();
        body.addProperty("capturedAtMillis", System.currentTimeMillis());
        body.addProperty("elapsedRealtimeMs", SystemClock.elapsedRealtime());
        body.add("memory", memory());
        body.add("availableAudioOutputs", audioOutputs());
        Player player = manager == null ? null : manager.getPlayer();
        body.addProperty("released", player == null);
        if (player == null) return body;
        DecoderMode audioMode = manager.getDecoderMode(C.TRACK_TYPE_AUDIO);
        DecoderMode videoMode = manager.getDecoderMode(C.TRACK_TYPE_VIDEO);
        body.addProperty("playerId", Integer.toHexString(System.identityHashCode(player)));
        body.addProperty("engine", manager.getEngine() == PlayerSetting.ENGINE_MPV ? "mpv" : "ExoPlayer");
        addUrl(body, manager.getUrl());
        if (player.isCommandAvailable(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)) body.addProperty("positionMs", manager.getPosition());
        body.addProperty("playbackState", player.getPlaybackState());
        body.addProperty("playWhenReady", player.getPlayWhenReady());
        body.addProperty("audioSessionId", player.getAudioSessionId());
        body.addProperty("tunnelingSettingEnabled", DecodeSetting.isTunnelingEnabled());
        body.addProperty("passthroughAllowed", DecodeSetting.isAudioPassThrough());
        body.addProperty("audioDecoderMode", audioMode == null ? null : audioMode.name());
        body.addProperty("videoDecoderMode", videoMode == null ? null : videoMode.name());
        addAudioAttributes(body, player);
        body.add("selectedTracks", selectedTracks(player));
        return body;
    }

    private static void addAudioAttributes(JsonObject body, Player player) {
        if (player.isCommandAvailable(Player.COMMAND_GET_AUDIO_ATTRIBUTES)) {
            AudioAttributes attributes = player.getAudioAttributes();
            JsonObject audio = new JsonObject();
            audio.addProperty("usage", attributes.usage);
            audio.addProperty("contentType", attributes.contentType);
            audio.addProperty("flags", attributes.flags);
            audio.addProperty("allowedCapturePolicy", attributes.allowedCapturePolicy);
            body.add("playerAudioAttributes", audio);
        }
    }

    @Nullable
    static JsonArray selectedTracks(Player player) {
        if (!player.isCommandAvailable(Player.COMMAND_GET_TRACKS)) return null;
        JsonArray tracks = new JsonArray();
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != C.TRACK_TYPE_AUDIO && group.getType() != C.TRACK_TYPE_VIDEO) continue;
            for (int i = 0; i < group.length; i++) {
                if (!group.isTrackSelected(i)) continue;
                JsonObject track = new JsonObject();
                track.addProperty("type", group.getType());
                addFormat(track, group.getTrackFormat(i));
                track.addProperty("supported", group.isTrackSupported(i));
                tracks.add(track);
            }
        }
        return tracks;
    }

    public static void recordError(JsonObject playback, PlaybackException error) {
        JsonObject body = playback.deepCopy();
        body.addProperty("errorCode", error.errorCode);
        body.addProperty("errorCodeName", error.getErrorCodeName());
        body.addProperty("errorElapsedRealtimeMs", error.timestampMs);
        JsonArray causes = new JsonArray();
        Throwable cause = error;
        for (int i = 0; cause != null && i < MAX_CAUSES; i++, cause = cause.getCause()) {
            causes.add(describeCause(cause));
        }
        body.add("causes", causes);
        body.addProperty("causesTruncated", cause != null);
        lastError = body;
    }

    private static JsonObject describeCause(Throwable cause) {
        JsonObject detail = new JsonObject();
        detail.addProperty("type", cause.getClass().getName());
        String message = cause.getMessage();
        detail.addProperty("message", limited(message, MAX_TEXT_LENGTH));
        detail.addProperty("messageTruncated", message != null && message.length() > MAX_TEXT_LENGTH);
        if (cause instanceof AudioSink.InitializationException failure) {
            detail.addProperty("audioTrackState", failure.audioTrackState);
            addFormat(detail, failure.format);
            detail.addProperty("recoverable", failure.isRecoverable);
        }
        return detail;
    }

    public static void addUrl(JsonObject body, String url) {
        body.addProperty("url", limited(url, MAX_URL_LENGTH));
        body.addProperty("urlTruncated", url != null && url.length() > MAX_URL_LENGTH);
    }

    private static void addFormat(JsonObject body, Format format) {
        String text = Format.toLogString(format);
        body.addProperty("format", limited(text, MAX_TEXT_LENGTH));
        body.addProperty("formatTruncated", text.length() > MAX_TEXT_LENGTH);
    }

    @Nullable
    public static JsonObject getLastError() {
        JsonObject error = lastError;
        return error == null ? null : error.deepCopy();
    }

    private static JsonObject memory() {
        JsonObject body = new JsonObject();
        Runtime runtime = Runtime.getRuntime();
        body.addProperty("heapUsedBytes", runtime.totalMemory() - runtime.freeMemory());
        body.addProperty("heapMaxBytes", runtime.maxMemory());
        ActivityManager manager = App.get().getSystemService(ActivityManager.class);
        if (manager == null) return body;
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        manager.getMemoryInfo(info);
        body.addProperty("systemAvailableBytes", info.availMem);
        body.addProperty("systemTotalBytes", info.totalMem);
        body.addProperty("systemLowMemory", info.lowMemory);
        return body;
    }

    private static JsonArray audioOutputs() {
        JsonArray devices = new JsonArray();
        AudioManager manager = App.get().getSystemService(AudioManager.class);
        if (manager == null) return devices;
        for (AudioDeviceInfo device : manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            JsonObject body = new JsonObject();
            body.addProperty("id", device.getId());
            body.addProperty("type", device.getType());
            devices.add(body);
        }
        return devices;
    }

    private static String limited(String value, int length) {
        if (value == null) return "";
        int end = Math.min(value.length(), length);
        if (end < value.length() && end > 0 && Character.isHighSurrogate(value.charAt(end - 1))) end--;
        return value.substring(0, end);
    }
}
