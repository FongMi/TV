package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

public class CustomKeyDownLive extends GestureDetector.SimpleOnGestureListener implements ScaleGestureDetector.OnScaleGestureListener {

    private static final int DISTANCE = 250;
    private static final int VELOCITY = 10;

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector detector;
    private final AudioManager manager;
    private final Listener listener;
    private final Activity activity;
    private final View videoView;
    private boolean changeBright;
    private boolean changeVolume;
    private boolean changeSpeed;
    private boolean changeScale;
    private boolean changeTime;
    private boolean animating;
    private boolean center;
    private boolean touch;
    private boolean lock;
    private float bright;
    private float volume;
    private float scale;
    private long time;

    public static CustomKeyDownLive create(Activity activity, View videoView) {
        return new CustomKeyDownLive(activity, videoView);
    }

    private CustomKeyDownLive(Activity activity, View videoView) {
        this.manager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        this.scaleDetector = new ScaleGestureDetector(activity, this);
        this.detector = new GestureDetector(activity, this);
        this.listener = (Listener) activity;
        this.videoView = videoView;
        this.activity = activity;
        this.scale = 1.0f;
    }

    public boolean onTouchEvent(MotionEvent e) {
        if (changeTime && e.getAction() == MotionEvent.ACTION_UP) onSeekEnd();
        if (changeSpeed && e.getAction() == MotionEvent.ACTION_UP) listener.onSpeedEnd();
        if (changeBright && e.getAction() == MotionEvent.ACTION_UP) listener.onBrightEnd();
        if (changeVolume && e.getAction() == MotionEvent.ACTION_UP) listener.onVolumeEnd();
        return e.getPointerCount() == 2 ? scaleDetector.onTouchEvent(e) : detector.onTouchEvent(e);
    }

    public void resetScale() {
        if (scale == 1.0f) return;
        videoView.animate().scaleX(1.0f).scaleY(1.0f).translationX(0f).translationY(0f).setDuration(250).withEndAction(() -> {
            videoView.setPivotY(videoView.getHeight() / 2f);
            videoView.setPivotX(videoView.getWidth() / 2f);
            scale = 1.0f;
        }).start();
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }

    public float getScale() {
        return scale;
    }

    private boolean isEdge(MotionEvent e) {
        return ResUtil.isEdge(activity, e, ResUtil.dp2px(24));
    }

    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        if (isEdge(e) || changeScale || lock || e.getPointerCount() > 1) return true;
        volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        bright = Util.getBrightness(activity);
        changeBright = false;
        changeVolume = false;
        changeSpeed = false;
        changeTime = false;
        center = false;
        touch = true;
        return true;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent e) {
        if (isEdge(e) || changeScale || lock || e.getPointerCount() > 1) return;
        changeSpeed = true;
        listener.onSpeedUp();
    }

    @Override
    public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
        if (isEdge(e1) || changeScale || lock || e1.getPointerCount() > 1) return true;
        float deltaX = e2.getX() - e1.getX();
        float deltaY = e1.getY() - e2.getY();
        if (touch) checkFunc(distanceX, distanceY, e2);
        if (changeTime) listener.onSeek(time = (long) (deltaX * 50));
        if (changeBright) setBright(deltaY);
        if (changeVolume) setVolume(deltaY);
        return true;
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent e) {
        if (isEdge(e) || changeScale || e.getPointerCount() > 1) return true;
        listener.onDoubleTap();
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
        if (isEdge(e) || changeScale || e.getPointerCount() > 1) return true;
        int half = ResUtil.getScreenWidth(activity) / 2;
        if (e.getX() > half || lock) listener.onDoubleTap();
        else listener.onSingleTap();
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
        if (isEdge(e1) || changeScale || !center || animating || e1.getPointerCount() > 1) return true;
        checkFunc(e1, e2, velocityX, velocityY);
        return true;
    }

    private void onSeekEnd() {
        listener.onSeekEnd(time);
        changeTime = false;
        time = 0;
    }

    private void checkFunc(float distanceX, float distanceY, MotionEvent e2) {
        int four = ResUtil.getScreenWidth(activity) / 4;
        if (e2.getX() > four && e2.getX() < four * 3) center = true;
        else if (Math.abs(distanceX) < Math.abs(distanceY)) checkSide(e2);
        if (Math.abs(distanceX) >= Math.abs(distanceY)) changeTime = true;
        touch = false;
    }

    private void checkFunc(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1.getX() - e2.getX() > DISTANCE && Math.abs(velocityX) > VELOCITY) {
            listener.onFlingLeft();
        } else if (e2.getX() - e1.getX() > DISTANCE && Math.abs(velocityX) > VELOCITY) {
            listener.onFlingRight();
        } else if (e1.getY() - e2.getY() > DISTANCE && Math.abs(velocityY) > VELOCITY) {
            videoView.animate().translationYBy(-ResUtil.dp2px(24)).setDuration(150).withStartAction(() -> animating = true).withEndAction(() -> videoView.animate().translationY(0).setDuration(100).withStartAction(listener::onFlingUp).withEndAction(() -> animating = false).start()).start();
        } else if (e2.getY() - e1.getY() > DISTANCE && Math.abs(velocityY) > VELOCITY) {
            videoView.animate().translationYBy(ResUtil.dp2px(24)).setDuration(150).withStartAction(() -> animating = true).withEndAction(() -> videoView.animate().translationY(0).setDuration(100).withStartAction(listener::onFlingDown).withEndAction(() -> animating = false).start()).start();
        }
    }

    private void checkSide(MotionEvent e2) {
        int half = ResUtil.getScreenWidth(activity) / 2;
        if (e2.getX() > half) changeVolume = true;
        else changeBright = true;
    }

    private void setBright(float deltaY) {
        if (bright == -1.0f) bright = 0.5f;
        int height = videoView.getMeasuredHeight();
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

    @Override
    public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
        if (changeBright || changeVolume || changeSpeed || changeTime || lock) return changeScale = false;
        return changeScale = true;
    }

    @Override
    public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
        App.post(() -> changeScale = false, 500);
    }

    @Override
    public boolean onScale(@NonNull ScaleGestureDetector detector) {
        scale *= detector.getScaleFactor();
        scale = Math.max(1.0f, Math.min(scale, 5.0f));
        videoView.setPivotX(detector.getFocusX());
        videoView.setPivotY(detector.getFocusY());
        videoView.setScaleX(scale);
        videoView.setScaleY(scale);
        return true;
    }

    public interface Listener {

        void onSpeedUp();

        void onSpeedEnd();

        void onBright(int progress);

        void onBrightEnd();

        void onVolume(int progress);

        void onVolumeEnd();

        void onFlingUp();

        void onFlingDown();

        void onFlingLeft();

        void onFlingRight();

        void onSeek(long time);

        void onSeekEnd(long time);

        void onSingleTap();

        void onDoubleTap();
    }
}
