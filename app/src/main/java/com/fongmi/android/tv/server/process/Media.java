package com.fongmi.android.tv.server.process;

import android.net.Uri;

import androidx.media3.common.MediaMetadata;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.PlaybackSnapshot;
import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.server.impl.Process;
import com.fongmi.android.tv.service.PlaybackService;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public class Media implements Process {

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return url.startsWith("/media");
    }

    @Override
    public Response doResponse(IHTTPSession session, String url, Map<String, String> files) {
        PlaybackService service = Server.get().getService();
        if (service == null) return Nano.ok("{}");
        CompletableFuture<String> future = new CompletableFuture<>();
        App.post(() -> future.complete(build(PlaybackSnapshot.capture(service.player())).toString()));
        try {
            return Nano.ok(future.get());
        } catch (Exception ignored) {
            return Nano.ok("{}");
        }
    }

    private JsonObject build(PlaybackSnapshot snapshot) {
        if (snapshot.released()) return new JsonObject();
        MediaMetadata meta = snapshot.playingMetadata();
        JsonObject result = new JsonObject();
        result.addProperty("state", snapshot.state());
        result.addProperty("speed", snapshot.speed());
        result.addProperty("duration", snapshot.duration());
        result.addProperty("position", snapshot.position());
        result.addProperty("url", snapshot.url());
        result.addProperty("title", getString(meta.title));
        result.addProperty("artist", getString(meta.artist));
        result.addProperty("artwork", getString(meta.artworkUri));
        return result;
    }

    private String getString(CharSequence text) {
        return text != null ? text.toString() : "";
    }

    private String getString(Uri uri) {
        return uri != null ? uri.toString() : "";
    }
}
