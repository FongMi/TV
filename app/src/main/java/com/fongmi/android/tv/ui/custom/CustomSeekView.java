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
    private Players player;

    private long currentDuration;
    private long currentPosition;
    private long currentBuffered;
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

    public void setListener(Players player) {
        positionView.setText(player.stringToTime(0));
        durationView.setText(player.stringToTime(0));
        this.player = player;
    }

    private void seekToTimeBarPosition(long positionMs) {
        player.seekTo(positionMs, true);
        updateProgress();
    }

    public void start() {
        removeCallbacks(runnable);
        post(runnable);
    }

    private void updateProgress() {
        if (player.isRelease()) return;
        long duration = player.getDuration();
        long position = player.getPosition();
        long buffered = player.getBuffered();
        boolean positionChanged = position != currentPosition;
        boolean durationChanged = duration != currentDuration;
        boolean bufferedChanged = buffered != currentBuffered;
        currentDuration = duration;
        currentPosition = position;
        currentBuffered = buffered;
        if (durationChanged) {
            timeBar.setDuration(duration);
            durationView.setText(player.stringToTime(duration < 0 ? 0 : duration));
        }
        if (positionChanged && !scrubbing) {
            timeBar.setPosition(position);
            positionView.setText(player.stringToTime(position < 0 ? 0 : position));
        }
        if (bufferedChanged) {
            timeBar.setBufferedPosition(buffered);
        }
        removeCallbacks(runnable);
        if (player.isPlaying()) {
            postDelayed(runnable, delayMs(position));
        } else {
            postDelayed(runnable, MAX_UPDATE_INTERVAL_MS);
        }
    }

    private long delayMs(long position) {
        long mediaTimeUntilNextFullSecondMs = 1000 - position % 1000;
        long mediaTimeDelayMs = Math.min(timeBar.getPreferredUpdateDelay(), mediaTimeUntilNextFullSecondMs);
        long delayMs = (long) (mediaTimeDelayMs / player.getSpeed());
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
        positionView.setText(player.stringToTime(position));
    }

    @Override
    public void onScrubMove(@NonNull TimeBar timeBar, long position) {
        positionView.setText(player.stringToTime(position));
    }

    @Override
    public void onScrubStop(@NonNull TimeBar timeBar, long position, boolean canceled) {
        scrubbing = false;
        if (!canceled) seekToTimeBarPosition(position);
    }
}
