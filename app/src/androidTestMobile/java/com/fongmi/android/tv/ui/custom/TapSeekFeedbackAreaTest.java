package com.fongmi.android.tv.ui.custom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
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
    public void sideSeekAreasUseOuterEighths() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        FrameLayout video = new FrameLayout(context);
        video.layout(0, 0, 1000, 500);
        TapSeekFeedback feedback = new TapSeekFeedback(video, () -> {});

        assertEquals(-1, feedback.directionAt(0f));
        assertEquals(-1, feedback.directionAt(124f));
        assertEquals(0, feedback.directionAt(125f));
        assertEquals(0, feedback.directionAt(200f));
        assertEquals(0, feedback.directionAt(800f));
        assertEquals(0, feedback.directionAt(874f));
        assertEquals(1, feedback.directionAt(875f));
        assertEquals(1, feedback.directionAt(999f));
    }

    @Test
    @UiThreadTest
    public void seekTextUses32DpSideInset() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        float density = context.getResources().getDisplayMetrics().density;
        int width = Math.round(800 * density);
        int height = Math.round(450 * density);
        int inset = Math.round(32 * density);
        FrameLayout video = new FrameLayout(context);
        video.layout(0, 0, width, height);
        TapSeekFeedback feedback = new TapSeekFeedback(video, () -> {});

        try {
            feedback.show(width - 1);
            layout(video, width, height);
            View text = feedback.getChildAt(0);
            assertTrue(feedback.getLeft() + (text.getLeft() + text.getRight()) / 2 >= width * 5 / 6);
            assertEquals(width - inset, feedback.getRight());

            feedback.show(0);
            layout(video, width, height);
            text = feedback.getChildAt(1);
            assertTrue(feedback.getLeft() + (text.getLeft() + text.getRight()) / 2 <= width / 6);
            assertEquals(inset, feedback.getLeft());
        } finally {
            feedback.clear();
        }
    }

    private static void layout(FrameLayout video, int width, int height) {
        video.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        video.layout(0, 0, width, height);
    }
}
