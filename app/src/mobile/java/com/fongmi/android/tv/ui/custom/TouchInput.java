package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.setting.LiveSetting;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

public final class TouchInput extends BaseInput<TouchInput.Listener> implements ScaleGestureDetector.OnScaleGestureListener {

    private static final int FLING_DISTANCE_PX = 100;

    private enum ScrollAction {PENDING, NONE, SEEK, BRIGHTNESS, VOLUME}

    private final ScaleGestureDetector scaleDetector;
    private final AudioManager manager;
    private final Activity activity;
    private final View videoView;
    private final Runnable endScaleCooldown = () -> scaling = false;
    private ScrollAction scrollAction = ScrollAction.PENDING;
    private boolean scaling;
    private boolean doubleTap;
    private boolean multiTouch;
    private boolean animating;
    private boolean locked;
    private float initialBrightness;
    private float initialVolume;
    private float scale;

    public static TouchInput create(Activity activity, View videoView, Listener listener) {
        return new TouchInput(activity, videoView, listener);
    }

    private TouchInput(Activity activity, View videoView, Listener listener) {
        super(activity, listener);
        this.manager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        this.scaleDetector = new ScaleGestureDetector(activity, this);
        this.videoView = videoView;
        this.activity = activity;
        this.scale = 1.0f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            finishSpeedPress();
            multiTouch = false;
            doubleTap = false;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN) multiTouch = true;
        boolean handled = super.onTouchEvent(e);
        handled = scaleDetector.onTouchEvent(e) || handled;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) finishGesture(action == MotionEvent.ACTION_UP);
        return handled;
    }

    public void resetScale() {
        if (scale == 1.0f) return;
        videoView.animate().scaleX(1.0f).scaleY(1.0f).translationX(0f).translationY(0f).setDuration(250).withEndAction(() -> {
            videoView.setPivotY(videoView.getHeight() / 2.0f);
            videoView.setPivotX(videoView.getWidth() / 2.0f);
            scale = 1.0f;
        }).start();
    }

    public void setLock(boolean locked) {
        this.locked = locked;
        if (locked) cancelSeekTaps();
    }

    public float getScale() {
        return scale;
    }

    private boolean isMultiTouch(MotionEvent e) {
        return e.getPointerCount() > 1;
    }

    private boolean isEdgeTouch(MotionEvent e) {
        return ResUtil.isEdge(activity, e, ResUtil.dp2px(24));
    }

    private boolean isOuterQuarter(MotionEvent e) {
        int quarter = ResUtil.getScreenWidth(activity) / 4;
        return e.getX() <= quarter || e.getX() >= quarter * 3;
    }

    private void beginGesture() {
        resetSeekTime();
        scrollAction = ScrollAction.PENDING;
        initialBrightness = Util.getBrightness(activity);
        initialVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void finishGesture(boolean commitSeek) {
        finishSpeedPress();
        if (commitSeek && scrollAction == ScrollAction.SEEK) listener.onSeekEnd(consumeSeekTime());
        else resetSeekTime();
        listener.onTouchEnd();
        scrollAction = ScrollAction.PENDING;
        doubleTap = false;
        multiTouch = false;
    }

    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        if (isMultiTouch(e) || isEdgeTouch(e) || scaling || locked) return true;
        beginGesture();
        return true;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent e) {
        if (doubleTap || multiTouch || isEdgeTouch(e) || scaling || locked || isSeekTapActive()) return;
        beginSpeedPress();
    }

    @Override
    public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
        if (multiTouch || isMultiTouch(e2) || isEdgeTouch(e1) || scaling || locked || isSpeedPressActive()) return true;
        float deltaX = e2.getX() - e1.getX();
        float deltaY = e1.getY() - e2.getY();
        if (scrollAction == ScrollAction.PENDING) scrollAction = selectScrollAction(Math.abs(deltaX), Math.abs(deltaY), e2);
        if (scrollAction == ScrollAction.SEEK) listener.onSeeking(setSeekTime((long) (deltaX * 50)));
        else if (scrollAction == ScrollAction.BRIGHTNESS) adjustBrightness(deltaY);
        else if (scrollAction == ScrollAction.VOLUME) adjustVolume(deltaY);
        return true;
    }

    @Override
    protected boolean acceptTap(MotionEvent e) {
        return !multiTouch && !isMultiTouch(e) && !isEdgeTouch(e) && !scaling;
    }

    @Override
    protected void onDoubleTapHandled() {
        doubleTap = true;
    }

    @Override
    protected boolean onSpeedPressBegin() {
        return listener.onSpeedPressStart();
    }

    @Override
    protected void onSpeedPressFinish() {
        listener.onSpeedPressEnd();
    }

    @Override
    public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
        if (multiTouch || isMultiTouch(e2) || isEdgeTouch(e1) || isOuterQuarter(e1) || scaling || hasScrollAction() || locked || animating) return true;
        animateVerticalFling(e1, e2);
        return true;
    }

    private boolean hasScrollAction() {
        return scrollAction != ScrollAction.PENDING && scrollAction != ScrollAction.NONE;
    }

    private ScrollAction selectScrollAction(float distanceX, float distanceY, MotionEvent event) {
        if ((float) Math.sqrt(distanceX * distanceX + distanceY * distanceY) < ResUtil.dp2px(20)) return ScrollAction.PENDING;
        if (distanceX >= distanceY) return ScrollAction.SEEK;
        if (!isOuterQuarter(event)) return ScrollAction.NONE;
        return event.getX() > ResUtil.getScreenWidth(activity) / 2 ? ScrollAction.VOLUME : ScrollAction.BRIGHTNESS;
    }

    private void animateVerticalFling(MotionEvent e1, MotionEvent e2) {
        float dx = e2.getX() - e1.getX();
        float dy = e2.getY() - e1.getY();
        double angle = Math.toDegrees(Math.atan2(Math.abs(dy), Math.abs(dx)));
        if (angle > 70 && e1.getY() - e2.getY() > FLING_DISTANCE_PX) {
            videoView.animate().translationYBy(ResUtil.dp2px(LiveSetting.isInvert() ? 24 : -24)).setDuration(150).withStartAction(() -> animating = true).withEndAction(() -> videoView.animate().translationY(0).setDuration(100).withStartAction(listener::onFlingUp).withEndAction(() -> animating = false).start()).start();
        } else if (angle > 70 && e2.getY() - e1.getY() > FLING_DISTANCE_PX) {
            videoView.animate().translationYBy(ResUtil.dp2px(LiveSetting.isInvert() ? -24 : 24)).setDuration(150).withStartAction(() -> animating = true).withEndAction(() -> videoView.animate().translationY(0).setDuration(100).withStartAction(listener::onFlingDown).withEndAction(() -> animating = false).start()).start();
        }
    }

    private void adjustBrightness(float deltaY) {
        int height = videoView.getMeasuredHeight();
        float brightness = deltaY * 2.0f / height + initialBrightness;
        if (brightness < 0) brightness = 0f;
        if (brightness > 1.0f) brightness = 1.0f;
        WindowManager.LayoutParams attributes = activity.getWindow().getAttributes();
        attributes.screenBrightness = brightness;
        activity.getWindow().setAttributes(attributes);
        listener.onBright((int) (brightness * 100));
    }

    private void adjustVolume(float deltaY) {
        int height = videoView.getMeasuredHeight();
        int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float deltaV = deltaY * 2.0f / height * maxVolume;
        float index = initialVolume + deltaV;
        if (index > maxVolume) index = maxVolume;
        if (index < 0) index = 0;
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) index, 0);
        listener.onVolume((int) (index / maxVolume * 100.0f));
    }

    @Override
    public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
        App.removeCallbacks(endScaleCooldown);
        if (hasScrollAction() || isSpeedPressActive() || locked) return scaling = false;
        return scaling = true;
    }

    @Override
    public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
        App.post(endScaleCooldown, 500);
    }

    @Override
    public boolean onScale(@NonNull ScaleGestureDetector detector) {
        scale *= detector.getScaleFactor();
        scale = Math.clamp(scale, 1.0f, 5.0f);
        videoView.setPivotX(detector.getFocusX());
        videoView.setPivotY(detector.getFocusY());
        videoView.setScaleX(scale);
        videoView.setScaleY(scale);
        return true;
    }

    public interface Listener extends BaseInput.Listener {

        @Override
        void onDoubleTap();

        void onSeeking(long time);

        void onSeekEnd(long time);

        boolean onSpeedPressStart();

        void onSpeedPressEnd();

        void onBright(int progress);

        void onVolume(int progress);

        void onFlingUp();

        void onFlingDown();

        void onTouchEnd();
    }
}
