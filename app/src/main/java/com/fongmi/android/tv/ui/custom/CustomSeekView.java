package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.player.Players;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.TimeBar;
import com.google.android.exoplayer2.util.Util;

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
        startProgress();
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

    public void setListener(Players listener) {
        this.listener = listener;
    }

    private void seekToTimeBarPosition(long positionMs) {
        listener.seekTo(positionMs);
        updateProgress();
    }

    public void startProgress() {
        stopProgress();
        post(runnable);
    }

    public void stopProgress() {
        removeCallbacks(runnable);
    }

    private void updateProgress() {
        long duration = listener.getDuration();
        long position = listener.getPosition();
        long buffered = listener.getBuffered();
        boolean positionChanged = position != currentPosition;
        boolean durationChanged = duration != currentDuration;
        currentDuration = duration;
        currentPosition = position;
        if (durationView != null && durationChanged) {
            durationView.setText(listener.stringToTime(duration));
        }
        if (timeBar != null && durationChanged) {
            timeBar.setDuration(duration);
        }
        if (positionView != null && !scrubbing && positionChanged) {
            positionView.setText(listener.stringToTime(position));
        }
        if (timeBar != null) {
            timeBar.setPosition(position);
            timeBar.setBufferedPosition(buffered);
        }
        removeCallbacks(runnable);
        if (listener.isPlaying()) {
            long mediaTimeDelayMs = timeBar != null ? timeBar.getPreferredUpdateDelay() : MAX_UPDATE_INTERVAL_MS;
            long mediaTimeUntilNextFullSecondMs = 1000 - position % 1000;
            mediaTimeDelayMs = Math.min(mediaTimeDelayMs, mediaTimeUntilNextFullSecondMs);
            float playbackSpeed = listener.getSpeed();
            long delayMs = playbackSpeed > 0 ? (long) (mediaTimeDelayMs / playbackSpeed) : MAX_UPDATE_INTERVAL_MS;
            delayMs = Util.constrainValue(delayMs, MIN_UPDATE_INTERVAL_MS, MAX_UPDATE_INTERVAL_MS);
            postDelayed(runnable, delayMs);
        } else {
            postDelayed(runnable, MAX_UPDATE_INTERVAL_MS);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopProgress();
    }

    @Override
    public void onScrubStart(@NonNull TimeBar timeBar, long position) {
        scrubbing = true;
        if (positionView != null) {
            positionView.setText(listener.stringToTime(position));
        }
    }

    @Override
    public void onScrubMove(@NonNull TimeBar timeBar, long position) {
        if (positionView != null) {
            positionView.setText(listener.stringToTime(position));
        }
    }

    @Override
    public void onScrubStop(@NonNull TimeBar timeBar, long position, boolean canceled) {
        scrubbing = false;
        if (!canceled) seekToTimeBarPosition(position);
    }
}
