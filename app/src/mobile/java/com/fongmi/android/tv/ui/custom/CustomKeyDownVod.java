package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.ResUtil;

public class CustomKeyDownVod extends GestureDetector.SimpleOnGestureListener {

    private final GestureDetector detector;
    private final AudioManager manager;
    private final Listener listener;
    private final Activity activity;
    private final View videoView;
    private boolean changeBright;
    private boolean changeVolume;
    private boolean changeSpeed;
    private boolean changeTime;
    private boolean touch;
    private boolean lock;
    private float bright;
    private float volume;
    private int time;

    public static CustomKeyDownVod create(Activity activity, View videoView) {
        return new CustomKeyDownVod(activity, videoView);
    }

    private CustomKeyDownVod(Activity activity, View videoView) {
        this.manager = (AudioManager) App.get().getSystemService(Context.AUDIO_SERVICE);
        this.detector = new GestureDetector(activity, this);
        this.listener = (Listener) activity;
        this.videoView = videoView;
        this.activity = activity;
    }

    public boolean onTouchEvent(MotionEvent e) {
        if (changeTime && e.getAction() == MotionEvent.ACTION_UP) onSeekEnd();
        if (changeSpeed && e.getAction() == MotionEvent.ACTION_UP) listener.onSpeedEnd();
        if (changeBright && e.getAction() == MotionEvent.ACTION_UP) listener.onBrightEnd();
        if (changeVolume && e.getAction() == MotionEvent.ACTION_UP) listener.onVolumeEnd();
        return detector.onTouchEvent(e);
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }

    private boolean isEdge(MotionEvent e) {
        return ResUtil.isEdge(e, ResUtil.dp2px(16));
    }

    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        if (isEdge(e) || lock) return true;
        volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        bright = activity.getWindow().getAttributes().screenBrightness;
        changeBright = false;
        changeVolume = false;
        changeSpeed = false;
        changeTime = false;
        touch = true;
        return true;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent e) {
        if (isEdge(e) || lock) return;
        changeSpeed = true;
        listener.onSpeedUp();
    }

    @Override
    public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
        if (isEdge(e1) || lock) return true;
        float deltaX = e2.getX() - e1.getX();
        float deltaY = e1.getY() - e2.getY();
        if (touch) checkFunc(distanceX, distanceY, e2);
        if (changeTime) listener.onSeek(time = (int) deltaX * 50);
        if (changeBright) setBright(deltaY);
        if (changeVolume) setVolume(deltaY);
        return true;
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent e) {
        if (!lock) listener.onDoubleTap();
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
        listener.onSingleTap();
        return true;
    }

    private void onSeekEnd() {
        listener.onSeekEnd(time);
        changeTime = false;
        time = 0;
    }

    private void checkFunc(float distanceX, float distanceY, MotionEvent e2) {
        changeTime = Math.abs(distanceX) >= Math.abs(distanceY);
        if (!changeTime) checkSide(e2);
        touch = false;
    }

    private void checkSide(MotionEvent e2) {
        int half = ResUtil.getScreenWidthNav() / 2;
        if (e2.getX() > half) changeVolume = true;
        else changeBright = true;
    }

    private void setBright(float deltaY) {
        int height = videoView.getMeasuredHeight();
        if (bright == -1.0f) bright = 0.5f;
        float brightness = deltaY * 2 / height + bright;
        if (brightness < 0) brightness = 0f;
        if (brightness > 1.0f) brightness = 1.0f;
        WindowManager.LayoutParams attributes = activity.getWindow().getAttributes();
        attributes.screenBrightness = brightness;
        activity.getWindow().setAttributes(attributes);
        listener.onBright((int) (brightness * 100));
    }

    private void setVolume(float deltaY) {
        int height = videoView.getMeasuredHeight();
        int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float deltaV = deltaY * 2 / height * maxVolume;
        float index = volume + deltaV;
        if (index > maxVolume) index = maxVolume;
        if (index < 0) index = 0;
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) index, 0);
        listener.onVolume((int) (index / maxVolume * 100));
    }

    public interface Listener {

        void onSpeedUp();

        void onSpeedEnd();

        void onBright(int progress);

        void onBrightEnd();

        void onVolume(int progress);

        void onVolumeEnd();

        void onSeek(int time);

        void onSeekEnd(int time);

        void onSingleTap();

        void onDoubleTap();
    }
}