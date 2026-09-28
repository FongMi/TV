package com.fongmi.android.tv.player.exo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.os.Build;
import android.widget.FrameLayout;

import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.fongmi.android.tv.player.engine.PlayerEngine;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ExoAssSubtitleIntegrationTest {

    @Test
    public void armDeviceAttachesAndDetachesNativeAssOverlay() {
        // The release publishes ARM APKs; an x86 emulator cannot load their JNI libraries.
        assumeTrue(Build.SUPPORTED_ABIS.length > 0 &&
                ("arm64-v8a".equals(Build.SUPPORTED_ABIS[0]) || "armeabi-v7a".equals(Build.SUPPORTED_ABIS[0])));

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            ExoPlayerSession session = new ExoPlayerSession(PlayerEngine.HARD, new Player.Listener() {}, null);
            try {
                PlayerView playerView = new PlayerView(context);
                FrameLayout overlay = playerView.getOverlayFrameLayout();
                assertNotNull(overlay);
                int initialChildren = overlay.getChildCount();
                session.bindPlayerView(playerView);
                assertEquals("The ARM libass JNI library must load", initialChildren + 1, overlay.getChildCount());
                session.bindPlayerView(null);
                assertEquals(initialChildren, overlay.getChildCount());
            } finally {
                session.release();
            }
        });
    }
}
