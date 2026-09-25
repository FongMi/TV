package com.fongmi.android.tv.api.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public final class NodePlaybackTest {

    @Test
    public void playInfoKeepsNodeRequestAndClearsIt() {
        NodePlayback playback = new NodePlayback();
        playback.setPlayInfo("line", "https://example.com/episode.m3u8");

        JsonObject info = JsonParser.parseString(playback.getPlayInfo()).getAsJsonObject();
        assertEquals("line", info.get("flag").getAsString());
        assertEquals("https://example.com/episode.m3u8", info.get("id").getAsString());
        assertEquals("episode.m3u8", info.get("fileName").getAsString());
        assertTrue(info.has("position"));
        assertTrue(info.has("duration"));

        playback.clear();
        JsonObject cleared = JsonParser.parseString(playback.getPlayInfo()).getAsJsonObject();
        assertEquals("", cleared.get("flag").getAsString());
        assertEquals("", cleared.get("id").getAsString());
    }

    @Test
    public void failedConfigLoadClearsPreviousPlayInfo() {
        NodeRuntime runtime = NodeRuntime.get();
        runtime.onPlayInfo("line", "episode");
        assertThrows(IOException.class, () -> runtime.loadConfig("invalid"));
        JsonObject info = JsonParser.parseString(runtime.onMessage("getPlayInfo", new JsonObject())).getAsJsonObject();
        assertEquals("", info.get("flag").getAsString());
        assertEquals("", info.get("id").getAsString());
    }
}
