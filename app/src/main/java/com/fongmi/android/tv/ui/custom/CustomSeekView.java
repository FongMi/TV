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

public class CustomSeekView extends FrameLayout implements TimeBar.OnScrubListener {

    private Players listener;
    private TextView positionView;
    private TextView durationView;
    private DefaultTimeBar timeBar;

    private boolean scrubbing;
    private boolean postProgress;

    public CustomSeekView(Context context) {
        this(context, null);
    }

    public CustomSeekView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomSeekView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_control_seek, this);
        positionView = findViewById(R.id.position);
        durationView = findViewById(R.id.duration);
        timeBar = findViewById(R.id.timeBar);
        timeBar.addListener(this);
        startProgress();
    }

    public void setListener(Players listener) {
        this.listener = listener;
    }

    private void seekToTimeBarPosition(long positionMs) {
        listener.seekTo(positionMs);
    }

    public void startProgress() {
        stopProgress();
        postProgress = true;
        post(runnable);
    }

    public void stopProgress() {
        postProgress = false;
        removeCallbacks(runnable);
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (listener.isPlaying()) {
                setProgress();
            }
            if (postProgress) {
                postDelayed(this, getDelay());
            }
        }
    };

    private long getDelay() {
        return (long) ((1000 - listener.getPosition() % 1000) / listener.getSpeed());
    }

    private void setProgress() {
        positionView.setText(listener.stringToTime(listener.getPosition()));
        durationView.setText(listener.stringToTime(listener.getDuration()));
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
