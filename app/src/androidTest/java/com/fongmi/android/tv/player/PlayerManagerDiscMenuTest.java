package com.fongmi.android.tv.player;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.ui.danmaku.DanmakuConfig;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.player.media.PlaySpec;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class PlayerManagerDiscMenuTest {

    @Test
    public void unresolvedNonIsoPlaySpecDoesNotExposeDiscMenu() {
        getInstrumentation().runOnMainSync(() -> {
            PlayerManager[] manager = new PlayerManager[1];
            boolean[] notified = new boolean[1];
            manager[0] = new PlayerManager(new PlayerManager.Callback() {
                @Override
                public void onDiscMenuAvailabilityChanged() {
                    if (manager[0] == null) return;
                    notified[0] = true;
                    assertNull(manager[0].getUrl());
                    assertFalse(manager[0].isDiscMenuAvailable());
                }

                @Override public void onPrepare() { fail("Unresolved URL must not prepare playback"); }

                @Override public void onTracksChanged() {}

                @Override public void onDecodeChanged() {}

                @Override public void onMediaOptionsChanged() {}

                @Override public void onError(String message) { fail(message); }

                @Override public boolean onRefresh() { return false; }

                @Override public void onPlayerRebuild(Player player) {}

                @Override public void onDanmakuSourceChanged(@Nullable Uri uri) {}

                @Override public void onDanmakuConfigChanged(DanmakuConfig config) {}

                @Override public void onDanmakuEnabledChanged(boolean enabled) {}

                @Override public void onDanmakuSent(String text) {}
            });
            try {
                Result result = Result.empty();
                result.setFormat(MimeTypes.VIDEO_MP4);
                manager[0].start(PlaySpec.fromParse(result, "key", MediaMetadata.EMPTY), 0);
                assertTrue(notified[0]);
            } finally {
                manager[0].release();
            }
        });
    }
}
