package com.fongmi.android.tv.server.process;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;

import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.server.impl.Process;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD;

public class Media implements Process {

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String url) {
        return url.startsWith("/media");
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String url, Map<String, String> files) {
        if (isNull()) return Nano.ok("{}");
        JsonObject result = new JsonObject();
        result.addProperty("url", getUrl());
        result.addProperty("state", getState());
        result.addProperty("speed", getSpeed());
        result.addProperty("title", getTitle());
        result.addProperty("artist", getArtist());
        result.addProperty("artwork", getArtUri());
        result.addProperty("duration", getDuration());
        result.addProperty("position", getPosition());
        return Nano.ok(result.toString());
    }

    private Players getPlayer() {
        return Server.get().getPlayer();
    }

    private boolean isNull() {
        return Objects.isNull(getPlayer()) || Objects.isNull(getPlayer().getSession());
    }

    private PlaybackStateCompat getPlaybackState() {
        return getPlayer().getSession().getController().getPlaybackState();
    }

    private MediaMetadataCompat getMetadata() {
        return getPlayer().getSession().getController().getMetadata();
    }

    private String getUrl() {
        return TextUtils.isEmpty(getPlayer().getUrl()) ? "" : getPlayer().getUrl();
    }

    private String getTitle() {
        return getMetadata() == null || getMetadata().getString(MediaMetadataCompat.METADATA_KEY_TITLE).isEmpty() ? "" : getMetadata().getString(MediaMetadataCompat.METADATA_KEY_TITLE);
    }

    private String getArtist() {
        return getMetadata() == null || getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ARTIST).isEmpty() ? "" : getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
    }

    private String getArtUri() {
        return getMetadata() == null ? "" : getMetadata().getString(MediaMetadataCompat.METADATA_KEY_ART_URI);
    }

    private long getDuration() {
        return getMetadata() == null ? -1 : getMetadata().getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
    }

    private int getState() {
        return getPlaybackState() == null ? -1 : getPlaybackState().getState();
    }

    private long getPosition() {
        return getPlaybackState() == null ? -1 : getPlaybackState().getPosition();
    }

    private float getSpeed() {
        return getPlaybackState() == null ? -1 : getPlaybackState().getPlaybackSpeed();
    }
}