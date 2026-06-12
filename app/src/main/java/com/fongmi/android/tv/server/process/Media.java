package com.fongmi.android.tv.server.process;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;

import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.browse.BrowseTree;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.server.impl.Process;
import com.fongmi.android.tv.service.PlaybackService;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public class Media implements Process {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return url.startsWith("/media");
    }

    @Override
    public Response doResponse(IHTTPSession session, String url, Map<String, String> files) {
        AtomicReference<String> result = new AtomicReference<>("");
        CountDownLatch latch = new CountDownLatch(1);

        mainHandler.post(() -> {
            try {
                result.set(getMediaInfo());
            } finally {
                latch.countDown();
            }
        });

        try {
            if (!latch.await(1, TimeUnit.SECONDS)) {
                return Nano.error("Timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Nano.error("Interrupted");
        }

        return Nano.ok(result.get());
    }

    private static String lastEpisodeId = "";
    private static long episodeSeq = 0;

    private String getMediaInfo() {
        PlaybackService service = Server.get().getService();
        if (service == null) return "{}";
        PlayerManager player = service.player();
        MediaItem item = player.getCurrentMediaItem();
        if (item == null) return "{}";
        MediaMetadata meta = item.mediaMetadata;

        String episodeId = "";
        if (meta.extras != null) {
            String value = meta.extras.getString("episodeId");
            if (value != null) episodeId = value;
        }

        if (!episodeId.equals(lastEpisodeId)) {
            lastEpisodeId = episodeId;
            episodeSeq++;
        }

        JsonObject result = new JsonObject();
        result.addProperty("state", getState(player));
        result.addProperty("speed", player.getSpeed());
        result.addProperty("duration", player.getDuration());
        result.addProperty("position", player.getPosition());
        result.addProperty("url", getString(player.getUrl()));
        result.addProperty("title", getString(meta.title));
        result.addProperty("artist", getString(meta.artist));
        result.addProperty("artwork", getString(meta.artworkUri));
        result.addProperty("mediaId", item.mediaId);
        result.addProperty("episodeSeq", episodeSeq);

        if (item.mediaId != null && item.mediaId.contains("@@@")) {
            String[] parts = item.mediaId.split("@@@", 3);
            if (parts.length >= 1) result.addProperty("siteKey", parts[0]);
            if (parts.length >= 2) result.addProperty("vodId", parts[1]);
        }

        result.addProperty("episodeId", episodeId);
        return result.toString();
    }

    private int getState(PlayerManager player) {
        if (player.isPlaying()) return 3;
        int state = player.getPlaybackState();
        if (state == Player.STATE_BUFFERING) return 6;
        if (state == Player.STATE_READY) return 2;
        return 1;
    }

    private String getString(CharSequence text) {
        return text != null ? text.toString() : "";
    }

    private String getString(Uri uri) {
        return uri != null ? uri.toString() : "";
    }
}