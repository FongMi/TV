package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;

public class CustomKeyDownVod extends GestureDetector.SimpleOnGestureListener {

    private final GestureDetector detector;
    private final AudioManager manager;
    private final Listener listener;
    private final Activity activity;
    private final View videoView;
    private boolean changeBright;
    private boolean changeVolume;
    private boolean touch;
    private boolean full;
    private float bright;
    private float volume;
    private int holdTime;

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
        if (!full) return false;
        if (changeBright && e.getAction() == MotionEvent.ACTION_UP) listener.onBrightEnd();
        if (changeVolume && e.getAction() == MotionEvent.ACTION_UP) listener.onVolumeEnd();
        return detector.onTouchEvent(e);
    }

    public void setFull(boolean full) {
        this.full = full;
    }

    public boolean hasEvent(KeyEvent event) {
        return Utils.isEnterKey(event) || Utils.isUpKey(event) || Utils.isDownKey(event) || Utils.isLeftKey(event) || Utils.isRightKey(event);
    }

    public boolean onKeyDown(KeyEvent event) {
        check(event);
        return true;
    }

    private void check(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && Utils.isLeftKey(event)) {
            listener.onSeeking(subTime());
        } else if (event.getAction() == KeyEvent.ACTION_DOWN && Utils.isRightKey(event)) {
            listener.onSeeking(addTime());
        } else if (event.getAction() == KeyEvent.ACTION_UP && (Utils.isLeftKey(event) || Utils.isRightKey(event))) {
            App.post(() -> listener.onSeekTo(holdTime), 250);
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isUpKey(event)) {
            listener.onKeyUp();
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isDownKey(event)) {
            listener.onKeyDown();
        } else if (event.getAction() == KeyEvent.ACTION_UP && Utils.isEnterKey(event)) {
            listener.onKeyCenter();
        }
    }

    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        if (!full) return false;
        volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        bright = activity.getWindow().getAttributes().screenBrightness;
        changeBright = false;
        changeVolume = false;
        touch = true;
        return true;
    }

    @Override
    public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
        if (!full) return false;
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
        listener.onSingleTap();
        return true;
    }

    private int addTime() {
        return holdTime = holdTime + Constant.INTERVAL_SEEK;
    }

    private int subTime() {
        return holdTime = holdTime - Constant.INTERVAL_SEEK;
    }

    public void resetTime() {
        holdTime = 0;
    }

    private void checkFunc(float distanceX, float distanceY, MotionEvent e2) {
        if (Math.abs(distanceX) < Math.abs(distanceY)) checkSide(e2);
        touch = false;
    }

    private void checkSide(MotionEvent e2) {
        int half = ResUtil.getScreenWidthNav() / 2;
        if (e2.getX() > half) {
            changeVolume = true;
        } else {
            changeBright = true;
        }
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

        void onSeeking(int time);

        void onSeekTo(int time);

        void onKeyUp();

        void onKeyDown();

        void onKeyCenter();

        void onSingleTap();

        void onDoubleTap();
    }
}
