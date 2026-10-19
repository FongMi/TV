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

import java.util.TimeZone;

public final class PlaybackLog {

    private static final int MAX_URL_LENGTH = 16 * 1024;
    private static final int MAX_CAUSES = 8;
    private static final int MAX_TEXT_LENGTH = 1024;

    private static volatile JsonObject lastError;

    public static JsonObject capture(PlayerManager manager) {
        JsonObject body = new JsonObject();
        addCaptureTime(body, System.currentTimeMillis(), TimeZone.getDefault());
        body.addProperty("elapsedRealtimeMs", SystemClock.elapsedRealtime());
        body.add("memory", memory());
        body.add("availableAudioOutputs", audioOutputs(App.get().getSystemService(AudioManager.class)));
        addPlayer(body, manager);
        return body;
    }

    static void addCaptureTime(JsonObject body, long timeMillis, TimeZone zone) {
        body.addProperty("capturedAtMillis", timeMillis);
        body.addProperty("timeZoneId", zone.getID());
        body.addProperty("utcOffsetMinutes", zone.getOffset(timeMillis) / 60000);
    }

    static void addPlayer(JsonObject body, PlayerManager manager) {
        Player player = manager == null ? null : manager.getPlayer();
        body.addProperty("playerPresent", player != null);
        if (manager != null) {
            body.addProperty("playbackId", manager.getPlaybackId());
            body.addProperty("playbackAttempt", manager.getPlaybackAttempt());
        }
        body.add("engineRuntime", manager == null ? null : manager.getPlaybackLog());
        if (player == null) return;
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
            for (int i = 0; i < group.length; i++) if (group.isTrackSelected(i)) tracks.add(describeTrack(group, i));
        }
        return tracks;
    }

    private static JsonObject describeTrack(Tracks.Group group, int index) {
        JsonObject track = new JsonObject();
        track.addProperty("type", group.getType());
        track.addProperty("typeName", group.getType() == C.TRACK_TYPE_AUDIO ? "audio" : "video");
        addFormat(track, group.getTrackFormat(index));
        track.addProperty("supported", group.isTrackSupported(index));
        return track;
    }

    public static void recordError(JsonObject playback, PlaybackException error) {
        JsonObject body = playback.deepCopy();
        body.addProperty("errorCode", error.errorCode);
        body.addProperty("errorCodeName", error.getErrorCodeName());
        body.addProperty("errorElapsedRealtimeMs", error.timestampMs);
        JsonArray causes = new JsonArray();
        Throwable cause = error;
        for (int i = 0; cause != null && i < MAX_CAUSES; i++, cause = cause.getCause()) causes.add(describeCause(cause));
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
        addText(body, "sampleMimeType", format.sampleMimeType);
        addText(body, "codecs", format.codecs);
        addText(body, "language", format.language);
        if (format.width != Format.NO_VALUE) body.addProperty("width", format.width);
        if (format.height != Format.NO_VALUE) body.addProperty("height", format.height);
        if (format.frameRate != Format.NO_VALUE) body.addProperty("frameRate", format.frameRate);
        if (format.channelCount != Format.NO_VALUE) body.addProperty("channelCount", format.channelCount);
        if (format.sampleRate != Format.NO_VALUE) body.addProperty("sampleRate", format.sampleRate);
        if (format.bitrate != Format.NO_VALUE) body.addProperty("bitrate", format.bitrate);
    }

    private static void addText(JsonObject body, String name, String value) {
        if (value == null) return;
        body.addProperty(name, limited(value, MAX_TEXT_LENGTH));
        if (value.length() > MAX_TEXT_LENGTH) body.addProperty(name + "Truncated", true);
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

    @Nullable
    static JsonArray audioOutputs(@Nullable AudioManager manager) {
        if (manager == null) return null;
        JsonArray devices = new JsonArray();
        for (AudioDeviceInfo device : manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) devices.add(describeAudioOutput(device));
        return devices;
    }

    private static JsonObject describeAudioOutput(AudioDeviceInfo device) {
        JsonObject body = new JsonObject();
        body.addProperty("id", device.getId());
        body.addProperty("type", device.getType());
        body.addProperty("typeName", audioDeviceType(device.getType()));
        return body;
    }

    static String audioDeviceType(int type) {
        return switch (type) {
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "BUILTIN_EARPIECE";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "BUILTIN_SPEAKER";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE -> "BUILTIN_SPEAKER_SAFE";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "WIRED_HEADPHONES";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BLUETOOTH_SCO";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BLUETOOTH_A2DP";
            case AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE_HEADSET";
            case AudioDeviceInfo.TYPE_BLE_SPEAKER -> "BLE_SPEAKER";
            case AudioDeviceInfo.TYPE_BLE_BROADCAST -> "BLE_BROADCAST";
            case AudioDeviceInfo.TYPE_HDMI -> "HDMI";
            case AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI_ARC";
            case AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI_EARC";
            case AudioDeviceInfo.TYPE_USB_DEVICE -> "USB_DEVICE";
            case AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB_ACCESSORY";
            case AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET";
            case AudioDeviceInfo.TYPE_HEARING_AID -> "HEARING_AID";
            case AudioDeviceInfo.TYPE_LINE_ANALOG -> "LINE_ANALOG";
            case AudioDeviceInfo.TYPE_LINE_DIGITAL -> "LINE_DIGITAL";
            case AudioDeviceInfo.TYPE_DOCK -> "DOCK";
            case AudioDeviceInfo.TYPE_AUX_LINE -> "AUX_LINE";
            case AudioDeviceInfo.TYPE_FM -> "FM";
            case AudioDeviceInfo.TYPE_IP -> "IP";
            case AudioDeviceInfo.TYPE_BUS -> "BUS";
            default -> "UNKNOWN";
        };
    }

    private static String limited(String value, int length) {
        if (value == null) return "";
        int end = Math.min(value.length(), length);
        if (end < value.length() && end > 0 && Character.isHighSurrogate(value.charAt(end - 1))) end--;
        return value.substring(0, end);
    }
}
