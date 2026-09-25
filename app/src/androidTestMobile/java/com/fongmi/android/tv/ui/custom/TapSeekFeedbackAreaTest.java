package com.fongmi.android.tv.ui.custom;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.widget.FrameLayout;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class TapSeekFeedbackAreaTest {

    @Test
    @UiThreadTest
    public void sideSeekAreasUseOuterFifths() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        FrameLayout video = new FrameLayout(context);
        video.layout(0, 0, 1000, 500);
        TapSeekFeedback feedback = new TapSeekFeedback(video, () -> {});

        assertEquals(-1, feedback.directionAt(0f));
        assertEquals(-1, feedback.directionAt(199f));
        assertEquals(0, feedback.directionAt(200f));
        assertEquals(0, feedback.directionAt(799f));
        assertEquals(1, feedback.directionAt(800f));
        assertEquals(1, feedback.directionAt(999f));
    }
}
