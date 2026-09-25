package com.fongmi.android.tv.ui.custom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class BaseInputSeekTapTest {

    @Test
    @UiThreadTest
    public void middleDoubleTapFallsBackToPlayback() {
        assertDoubleTap(false, false, 0, 1, false);
    }

    @Test
    @UiThreadTest
    public void unavailableSideDoubleTapDoesNotTogglePlaybackOrStartAccumulation() {
        assertDoubleTap(true, false, 1, 0, false);
    }

    @Test
    @UiThreadTest
    public void successfulSideDoubleTapStartsAccumulationWithoutTogglingPlayback() {
        assertDoubleTap(true, true, 1, 0, true);
    }

    @Test
    @UiThreadTest
    public void middleDoubleTapDuringAccumulationStillTogglesPlayback() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TestListener listener = new TestListener(true, true);
        TestInput input = new TestInput(context, listener);
        long time = SystemClock.uptimeMillis();
        MotionEvent sideDown = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        MotionEvent middleDown = MotionEvent.obtain(time + 100, time + 100, MotionEvent.ACTION_DOWN, 200f, 100f, 0);
        try {
            input.onTouchEvent(sideDown);
            input.onDoubleTap(sideDown);
            assertTrue(input.isSeekTapActive());
            listener.seekArea = false;
            input.onTouchEvent(middleDown);
            input.onDoubleTap(middleDown);
            assertEquals(1, listener.seekTaps);
            assertEquals(1, listener.playbackToggles);
            assertFalse(input.isSeekTapActive());
        } finally {
            sideDown.recycle();
            middleDown.recycle();
        }
    }

    @Test
    @UiThreadTest
    public void disabledDoubleTapDispatchesEachSingleTapImmediately() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TestListener listener = new TestListener(false, false);
        TestInput input = new TestInput(context, listener, false);
        long time = SystemClock.uptimeMillis();
        tap(input, time);
        assertEquals(1, listener.singleTaps);
        tap(input, time + 100);
        assertEquals(2, listener.singleTaps);
        assertEquals(0, listener.playbackToggles);
    }

    private static void tap(TestInput input, long time) {
        MotionEvent down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        MotionEvent up = MotionEvent.obtain(time, time + 20, MotionEvent.ACTION_UP, 100f, 100f, 0);
        try {
            input.onTouchEvent(down);
            input.onTouchEvent(up);
        } finally {
            down.recycle();
            up.recycle();
        }
    }

    private static void assertDoubleTap(boolean seekArea, boolean seekable, int seekTaps, int playbackToggles, boolean accumulating) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        TestListener listener = new TestListener(seekArea, seekable);
        TestInput input = new TestInput(context, listener);
        long time = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 100f, 100f, 0);
        try {
            input.onTouchEvent(down);
            input.onDoubleTap(down);
            assertEquals(seekTaps, listener.seekTaps);
            assertEquals(playbackToggles, listener.playbackToggles);
            if (accumulating) assertTrue(input.isSeekTapActive());
            else assertFalse(input.isSeekTapActive());
        } finally {
            down.recycle();
        }
    }

    private static final class TestInput extends BaseInput<TestListener> {

        TestInput(Context context, TestListener listener) {
            super(context, listener);
        }

        TestInput(Context context, TestListener listener, boolean doubleTapEnabled) {
            super(context, listener, doubleTapEnabled);
        }
    }

    private static final class TestListener implements BaseInput.Listener {

        private boolean seekArea;
        private final boolean seekable;
        private int playbackToggles;
        private int seekTaps;
        private int singleTaps;

        TestListener(boolean seekArea, boolean seekable) {
            this.seekArea = seekArea;
            this.seekable = seekable;
        }

        @Override
        public void onSingleTap() {
            singleTaps++;
        }

        @Override
        public void onDoubleTap() {
            playbackToggles++;
        }

        @Override
        public boolean isSeekTapArea(float x) {
            return seekArea;
        }

        @Override
        public boolean onSeekTap(float x) {
            seekTaps++;
            return seekable;
        }
    }
}
