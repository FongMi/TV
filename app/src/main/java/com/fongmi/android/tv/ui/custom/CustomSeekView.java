package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.util.Util;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.TimeBar;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.player.Players;

public class CustomSeekView extends FrameLayout implements TimeBar.OnScrubListener {

    private static final int MAX_UPDATE_INTERVAL_MS = 1000;
    private static final int MIN_UPDATE_INTERVAL_MS = 200;

    private TextView positionView;
    private TextView durationView;
    private DefaultTimeBar timeBar;

    private Runnable runnable;
    private Players listener;

    private long currentDuration;
    private long currentPosition;
    private boolean scrubbing;

    public CustomSeekView(Context context) {
        this(context, null);
    }

    public CustomSeekView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomSeekView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_control_seek, this);
        initView();
        initEvent();
        start();
    }

    private void initView() {
        positionView = findViewById(R.id.position);
        durationView = findViewById(R.id.duration);
        timeBar = findViewById(R.id.timeBar);
        runnable = this::updateProgress;
    }

    private void initEvent() {
        timeBar.addListener(this);
    }

    public void setListener(Players players) {
        listener = players;
        positionView.setText(listener.stringToTime(0));
        durationView.setText(listener.stringToTime(0));
    }

    private void seekToTimeBarPosition(long positionMs) {
        listener.seekTo(positionMs, true);
        updateProgress();
    }

    public void start() {
        removeCallbacks(runnable);
        post(runnable);
    }

    private void updateProgress() {
        if (listener.isRelease()) return;
        long duration = listener.getDuration();
        long position = listener.getPosition();
        long buffered = listener.getBuffered();
        boolean positionChanged = position != currentPosition;
        boolean durationChanged = duration != currentDuration;
        currentDuration = duration;
        currentPosition = position;
        if (durationChanged) {
            timeBar.setDuration(duration);
            durationView.setText(listener.stringToTime(duration < 0 ? 0 : duration));
        }
        if (positionChanged && !scrubbing) {
            timeBar.setPosition(position);
            timeBar.setBufferedPosition(buffered);
            positionView.setText(listener.stringToTime(position < 0 ? 0 : position));
        }
        removeCallbacks(runnable);
        if (listener.isPlaying()) {
            postDelayed(runnable, delayMs(position));
        } else {
            postDelayed(runnable, MAX_UPDATE_INTERVAL_MS);
        }
    }

    private long delayMs(long position) {
        long mediaTimeUntilNextFullSecondMs = 1000 - position % 1000;
        long mediaTimeDelayMs = Math.min(timeBar.getPreferredUpdateDelay(), mediaTimeUntilNextFullSecondMs);
        long delayMs = (long) (mediaTimeDelayMs / listener.getSpeed());
        return Util.constrainValue(delayMs, MIN_UPDATE_INTERVAL_MS, MAX_UPDATE_INTERVAL_MS);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(runnable);
    }

    @Override
    public void onScrubStart(@NonNull TimeBar timeBar, long position) {
        scrubbing = true;
        positionView.setText(listener.stringToTime(position));
    }

    @Override
    public void onScrubMove(@NonNull TimeBar timeBar, long position) {
        positionView.setText(listener.stringToTime(position));
    }

    @Override
    public void onScrubStop(@NonNull TimeBar timeBar, long position, boolean canceled) {
        scrubbing = false;
        if (!canceled) seekToTimeBarPosition(position);
    }
}
