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

public class CustomKeyDownLive extends GestureDetector.SimpleOnGestureListener {

    private static final int DISTANCE = 100;
    private static final int VELOCITY = 10;

    private final GestureDetector detector;
    private final AudioManager manager;
    private final Listener listener;
    private final Activity activity;
    private final View videoView;
    private boolean changeBright;
    private boolean changeVolume;
    private boolean center;
    private boolean touch;
    private boolean lock;
    private float bright;
    private float volume;

    public static CustomKeyDownLive create(Activity activity, View videoView) {
        return new CustomKeyDownLive(activity, videoView);
    }

    private CustomKeyDownLive(Activity activity, View videoView) {
        this.manager = (AudioManager) App.get().getSystemService(Context.AUDIO_SERVICE);
        this.detector = new GestureDetector(activity, this);
        this.listener = (Listener) activity;
        this.videoView = videoView;
        this.activity = activity;
    }

    public boolean onTouchEvent(MotionEvent e) {
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
        center = false;
        touch = true;
        return true;
    }

    @Override
    public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
        if (isEdge(e1) || lock) return true;
        float deltaY = e1.getY() - e2.getY();
        if (touch) checkFunc(distanceX, distanceY, e2);
        if (changeBright) setBright(deltaY);
        if (changeVolume) setVolume(deltaY);
        return true;
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent e) {
        listener.onDoubleTap();
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
        int half = ResUtil.getScreenWidthNav() / 2;
        if (e.getX() > half || lock) listener.onDoubleTap();
        else listener.onSingleTap();
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (isEdge(e1) || !center) return true;
        checkFunc(e1, e2, velocityX, velocityY);
        return true;
    }

    private void checkFunc(float distanceX, float distanceY, MotionEvent e2) {
        int four = ResUtil.getScreenWidthNav() / 4;
        if (e2.getX() > four && e2.getX() < four * 3) center = true;
        else if (Math.abs(distanceX) < Math.abs(distanceY)) checkSide(e2);
        touch = false;
    }

    private void checkFunc(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1.getX() - e2.getX() > DISTANCE && Math.abs(velocityX) > VELOCITY) {
            listener.onFlingLeft();
        } else if (e2.getX() - e1.getX() > DISTANCE && Math.abs(velocityX) > VELOCITY) {
            listener.onFlingRight();
        } else if (e1.getY() - e2.getY() > DISTANCE && Math.abs(velocityY) > VELOCITY) {
            listener.onFlingUp();
        } else if (e2.getY() - e1.getY() > DISTANCE && Math.abs(velocityY) > VELOCITY) {
            listener.onFlingDown();
        }
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

        void onBright(int progress);

        void onBrightEnd();

        void onVolume(int progress);

        void onVolumeEnd();

        void onFlingUp();

        void onFlingDown();

        void onFlingLeft();

        void onFlingRight();

        void onSingleTap();

        void onDoubleTap();
    }
}
