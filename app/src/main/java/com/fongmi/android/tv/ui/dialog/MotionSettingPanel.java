package com.fongmi.android.tv.ui.dialog;

import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.decoder.ffmpeg.FfmpegMotionInterpolationController;
import androidx.media3.mpvplayer.MpvPlayer;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ViewSettingSliderBinding;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.SliderUtil;
import com.github.catvod.utils.Task;
import com.google.android.material.slider.Slider;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.CancellationException;

final class MotionSettingPanel implements Slider.OnSliderTouchListener {

    private static final int FPS_STEP = 60;
    private static final int MAX_FPS = 120;

    private final ViewSettingSliderBinding binding;
    @Nullable
    private final MpvPlayer mpv;
    @Nullable
    private final FfmpegMotionInterpolationController exo;
    private final Runnable motionListener = this::updateMotion;
    private final Slider.OnChangeListener changeListener = this::onMotionChanged;
    private TouchState touchState = TouchState.IDLE;
    @Nullable private Integer pendingFps;
    private boolean released;

    MotionSettingPanel(ViewSettingSliderBinding binding, @Nullable Player player, @Nullable FfmpegMotionInterpolationController exo) {
        this.binding = binding;
        this.mpv = player instanceof MpvPlayer mpvPlayer ? mpvPlayer : null;
        this.exo = exo;
    }

    void bind() {
        boolean available = hasController();
        binding.getRoot().setVisibility(available ? View.VISIBLE : View.GONE);
        if (!available) return;
        binding.title.setText(R.string.player_motion);
        binding.value.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        binding.slider.setContentDescription(binding.title.getText());
        binding.slider.setValueFrom(0);
        binding.slider.setValueTo(MAX_FPS);
        binding.slider.setStepSize(FPS_STEP);
        binding.slider.setLabelFormatter(this::formatRate);
        binding.slider.addOnChangeListener(changeListener);
        binding.slider.addOnSliderTouchListener(this);
        if (mpv != null) mpv.addMotionInterpolationListener(motionListener);
        else exo.addListener(motionListener);
        updateMotion();
    }

    void release() {
        released = true;
        binding.slider.setLabelFormatter(null);
        binding.slider.removeOnChangeListener(changeListener);
        binding.slider.removeOnSliderTouchListener(this);
        if (mpv != null) mpv.removeMotionInterpolationListener(motionListener);
        else if (exo != null) exo.removeListener(motionListener);
    }

    @Override
    public void onStartTrackingTouch(@NonNull Slider slider) {
        if (touchState != TouchState.IDLE) return;
        touchState = canChangeMotion() ? TouchState.TRACKING : TouchState.BLOCKED;
    }

    @Override
    public void onStopTrackingTouch(@NonNull Slider slider) {
        TouchState finished = touchState;
        touchState = TouchState.IDLE;
        if (finished == TouchState.TRACKING) applyMotion(Math.round(slider.getValue()));
        else if (finished == TouchState.BLOCKED) updateMotion();
    }

    private void onMotionChanged(@NonNull Slider slider, float value, boolean fromUser) {
        if (!fromUser) return;
        switch (touchState) {
            case IDLE -> applyMotion(Math.round(value));
            case TRACKING -> setValueText(formatRate(value));
            case BLOCKED -> updateMotion();
        }
    }

    private void applyMotion(int fps) {
        if (!canChangeMotion() || fps == getTargetFps()) {
            updateMotion();
            return;
        }
        pendingFps = fps;
        ListenableFuture<Void> update = mpv != null ? mpv.setMotionInterpolationFps(fps) : exo.setTargetFps(fps);
        Futures.addCallback(update, Task.callback(result -> onApplied(fps), this::onFailed), MoreExecutors.directExecutor());
    }

    private void onApplied(int fps) {
        pendingFps = null;
        PlayerSetting.putMotion(fps / FPS_STEP);
        updateMotion();
    }

    private void onFailed(Throwable error) {
        pendingFps = null;
        if (!(error instanceof CancellationException)) Notify.show(Notify.getError(R.string.player_motion_failed, error));
        updateMotion();
    }

    private void updateMotion() {
        if (released || !hasController()) return;
        boolean supported = isSupported();
        if (touchState == TouchState.TRACKING) {
            if (supported && !isUpdatePending()) return;
            touchState = TouchState.BLOCKED;
        }
        int fps = pendingFps == null ? getTargetFps() : pendingFps;
        binding.slider.setEnabled(supported || touchState == TouchState.BLOCKED);
        SliderUtil.setValue(binding.slider, fps);
        setValueText(getValueText(supported, fps));
    }

    private boolean hasController() {
        return mpv != null || exo != null;
    }

    private boolean canChangeMotion() {
        return isSupported() && !isUpdatePending();
    }

    private boolean isSupported() {
        return mpv != null ? mpv.isMotionInterpolationSupported() : exo != null && exo.isSupported();
    }

    private boolean isUpdatePending() {
        return pendingFps != null || (mpv != null ? mpv.isMotionInterpolationUpdatePending() : exo != null && exo.isUpdatePending());
    }

    private int getTargetFps() {
        return mpv != null ? mpv.getMotionInterpolationFps() : exo.getTargetFps();
    }

    private String formatRate(float value) {
        int fps = Math.round(value);
        return fps == 0 ? ResUtil.getString(R.string.setting_off) : ResUtil.getString(R.string.player_motion_fps, fps);
    }

    private String getValueText(boolean supported, int fps) {
        if (!supported) return ResUtil.getString(R.string.player_motion_unavailable);
        if (mpv != null && pendingFps == null) {
            float effectiveFps = mpv.getMotionInterpolationEffectiveTargetFps();
            if (effectiveFps > 0 && effectiveFps < fps - 0.001f) return ResUtil.getString(R.string.player_motion_capped, effectiveFps);
        }
        return formatRate(fps);
    }

    private void setValueText(String text) {
        if (!TextUtils.equals(binding.value.getText(), text)) binding.value.setText(text);
    }

    private enum TouchState {
        IDLE, TRACKING, BLOCKED
    }
}
