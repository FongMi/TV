package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.content.Context;
import android.graphics.RectF;
import android.media.AudioManager;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.media3.common.VideoTransform;
import androidx.media3.ui.PlayerView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.setting.LiveSetting;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

public final class TouchInput extends BaseInput<TouchInput.Listener> implements ScaleGestureDetector.OnScaleGestureListener {

    private static final int FLING_DISTANCE_PX = 100;

    private enum ScrollAction {PENDING, NONE, SEEK, BRIGHTNESS, VOLUME}

    private final ScaleGestureDetector scaleDetector;
    private final PinchScale pinchScale = new PinchScale();
    private final RectF pinchContentBounds = new RectF();
    private final AudioManager manager;
    private final Activity activity;
    private final PlayerView videoView;
    private final Runnable endScaleCooldown = () -> scaling = false;
    private ScrollAction scrollAction = ScrollAction.PENDING;
    private boolean scaling;
    private boolean doubleTap;
    private boolean multiTouch;
    private boolean animating;
    private boolean locked;
    private float pivotX;
    private float pivotY;
    private float translationX;
    private float translationY;
    private float initialBrightness;
    private float initialVolume;

    public static TouchInput create(Activity activity, PlayerView videoView, Listener listener) {
        return new TouchInput(activity, videoView, listener);
    }

    private TouchInput(Activity activity, PlayerView videoView, Listener listener) {
        super(activity, listener);
        this.manager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        this.scaleDetector = new ScaleGestureDetector(activity, this);
        this.scaleDetector.setQuickScaleEnabled(false);
        this.videoView = videoView;
        this.activity = activity;
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
        App.removeCallbacks(endScaleCooldown);
        scaling = false;
        pinchScale.reset();
        listener.onPinchScaleEnd();
        if (!videoView.getVideoTransform().equals(VideoTransform.DEFAULT)) videoView.animateVideoTransform(VideoTransform.DEFAULT, 250);
    }

    public void setLock(boolean locked) {
        this.locked = locked;
        if (locked) cancelSeekTaps();
    }

    public boolean isScaled() {
        return !videoView.getVideoTransform().equals(VideoTransform.DEFAULT);
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
        if (angle > 70 && e1.getY() - e2.getY() > FLING_DISTANCE_PX) animateVerticalFling(LiveSetting.isInvert() ? 24 : -24, listener::onFlingUp);
        else if (angle > 70 && e2.getY() - e1.getY() > FLING_DISTANCE_PX) animateVerticalFling(LiveSetting.isInvert() ? -24 : 24, listener::onFlingDown);
    }

    private void animateVerticalFling(int distance, Runnable action) {
        videoView.animate().translationYBy(ResUtil.dp2px(distance)).setDuration(150)
                .withStartAction(() -> animating = true)
                .withEndAction(() -> finishVerticalFling(action))
                .start();
    }

    private void finishVerticalFling(Runnable action) {
        videoView.animate().translationY(0).setDuration(100)
                .withStartAction(action)
                .withEndAction(() -> animating = false)
                .start();
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
        if (!listener.isScaleEnabled() || hasScrollAction() || isSpeedPressActive() || locked || animating) return scaling = false;
        videoView.finishVideoTransformAnimation();
        VideoTransform transform = videoView.getVideoTransform();
        pinchScale.setScale(transform.scaleX, transform.scaleY);
        pinchContentBounds.set(videoView.getVideoBounds());
        pivotX = pinchContentBounds.centerX();
        pivotY = pinchContentBounds.centerY();
        translationX = transform.panX * pinchContentBounds.width() * transform.scaleX;
        translationY = transform.panY * pinchContentBounds.height() * transform.scaleY;
        setPinchPivot(detector);
        beginPinchScale(detector);
        return scaling = true;
    }

    private void setPinchPivot(ScaleGestureDetector detector) {
        float focusX = detector.getFocusX() - videoView.getLeft();
        float focusY = detector.getFocusY() - videoView.getTop();
        if (PinchScale.useCenterPivot(multiTouch, detector.getCurrentSpanX(), detector.getCurrentSpanY(), focusX, focusY, videoView.getWidth(), videoView.getHeight())) {
            focusX = videoView.getWidth() / 2.0f;
            focusY = videoView.getHeight() / 2.0f;
        }
        float localPivotX = PinchScale.localFocus(focusX, translationX, pivotX, pinchScale.getVisualScaleX());
        float localPivotY = PinchScale.localFocus(focusY, translationY, pivotY, pinchScale.getVisualScaleY());
        translationX = focusX - localPivotX;
        translationY = focusY - localPivotY;
        pivotX = localPivotX;
        pivotY = localPivotY;
    }

    private void beginPinchScale(ScaleGestureDetector detector) {
        RectF contentBounds = pinchContentBounds;
        if (contentBounds.isEmpty()) contentBounds.set(0, 0, videoView.getWidth(), videoView.getHeight());
        PinchScale.Axis axis = multiTouch ? PinchScale.axisForSpans(detector.getCurrentSpanX(), detector.getCurrentSpanY()) : PinchScale.Axis.BOTH;
        float edgeScaleX = axis == PinchScale.Axis.HORIZONTAL
                ? PinchScale.centeredEdgeScale(contentBounds.left, contentBounds.right, pivotX, videoView.getWidth(), translationX, pinchScale.getScaleX())
                : PinchScale.edgeContactScale(contentBounds.left, contentBounds.right, pivotX, videoView.getWidth(), translationX, pinchScale.getScaleX());
        float edgeScaleY = axis == PinchScale.Axis.VERTICAL
                ? PinchScale.centeredEdgeScale(contentBounds.top, contentBounds.bottom, pivotY, videoView.getHeight(), translationY, pinchScale.getScaleY())
                : PinchScale.edgeContactScale(contentBounds.top, contentBounds.bottom, pivotY, videoView.getHeight(), translationY, pinchScale.getScaleY());
        if (axis == PinchScale.Axis.HORIZONTAL) {
            float pivot = contentBounds.centerX();
            pivotX = pivot;
            translationX = videoView.getWidth() / 2.0f - pivot;
        } else if (axis == PinchScale.Axis.VERTICAL) {
            float pivot = contentBounds.centerY();
            pivotY = pivot;
            translationY = videoView.getHeight() / 2.0f - pivot;
        }
        pinchScale.begin(detector.getCurrentSpanX(), detector.getCurrentSpanY(), edgeScaleX, edgeScaleY);
    }

    @Override
    public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
        if (scaling && listener.isScaleEnabled()) {
            float releasedX = PinchScale.releaseTranslation(translationX, pinchContentBounds.left, pinchContentBounds.right, pivotX, pinchScale.getScaleX(), pinchScale.getVisualScaleX(), videoView.getWidth());
            float releasedY = PinchScale.releaseTranslation(translationY, pinchContentBounds.top, pinchContentBounds.bottom, pivotY, pinchScale.getScaleY(), pinchScale.getVisualScaleY(), videoView.getHeight());
            VideoTransform target = videoTransform(pinchScale.getScaleX(), pinchScale.getScaleY(), releasedX, releasedY);
            if (!target.equals(videoView.getVideoTransform())) videoView.animateVideoTransform(target, 180);
        }
        pinchScale.end();
        listener.onPinchScaleEnd();
        App.post(endScaleCooldown, 500);
    }

    @Override
    public boolean onScale(@NonNull ScaleGestureDetector detector) {
        if (!scaling || !listener.isScaleEnabled()) return scaling = false;
        float factor = detector.getScaleFactor();
        float previousScaleX = pinchScale.getScaleX();
        float previousScaleY = pinchScale.getScaleY();
        float previousVisualScaleX = pinchScale.getVisualScaleX();
        float previousVisualScaleY = pinchScale.getVisualScaleY();
        PinchScale.Axis axis = pinchScale.update(factor, multiTouch);
        if (axis != PinchScale.Axis.VERTICAL && PinchScale.shouldConstrainTranslation(previousScaleX, previousVisualScaleX, pinchScale.getScaleX(), pinchScale.getVisualScaleX())) translationX = PinchScale.constrainTranslation(translationX, pinchContentBounds.left, pinchContentBounds.right, pivotX, pinchScale.getVisualScaleX(), videoView.getWidth());
        if (axis != PinchScale.Axis.HORIZONTAL && PinchScale.shouldConstrainTranslation(previousScaleY, previousVisualScaleY, pinchScale.getScaleY(), pinchScale.getVisualScaleY())) translationY = PinchScale.constrainTranslation(translationY, pinchContentBounds.top, pinchContentBounds.bottom, pivotY, pinchScale.getVisualScaleY(), videoView.getHeight());
        videoView.setVideoTransform(videoTransform(pinchScale.getVisualScaleX(), pinchScale.getVisualScaleY(), translationX, translationY));
        listener.onPinchScale(axis, pinchScale.getScaleX(), pinchScale.getScaleY());
        return true;
    }

    private VideoTransform videoTransform(float scaleX, float scaleY, float translationX, float translationY) {
        float offsetX = translationX + (1 - scaleX) * (pivotX - pinchContentBounds.centerX());
        float offsetY = translationY + (1 - scaleY) * (pivotY - pinchContentBounds.centerY());
        return new VideoTransform(scaleX, scaleY,
                offsetX / (pinchContentBounds.width() * scaleX),
                offsetY / (pinchContentBounds.height() * scaleY));
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

        boolean isScaleEnabled();

        void onPinchScale(PinchScale.Axis axis, float scaleX, float scaleY);

        void onPinchScaleEnd();

        void onFlingUp();

        void onFlingDown();

        void onTouchEnd();
    }
}
