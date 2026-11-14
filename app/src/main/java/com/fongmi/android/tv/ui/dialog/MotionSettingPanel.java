package com.fongmi.android.tv.ui.dialog;

import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.decoder.ffmpeg.FfmpegMotionInterpolationController;
import androidx.media3.mpvplayer.MpvPlayer;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ViewMotionSettingBinding;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.utils.Task;
import com.google.android.material.chip.Chip;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.CancellationException;

final class MotionSettingPanel {

    private final ViewMotionSettingBinding binding;
    private final Chip[] options;
    @Nullable private final Player eventSource;
    @Nullable
    private final MpvPlayer mpv;
    @Nullable
    private final FfmpegMotionInterpolationController exo;
    private final Runnable motionListener = this::updateMotion;
    @Nullable private Integer pendingFps;
    private boolean released;

    MotionSettingPanel(ViewMotionSettingBinding binding, @Nullable Player eventSource, @Nullable FfmpegMotionInterpolationController exo) {
        this.binding = binding;
        this.options = new Chip[]{binding.motionOff, binding.motion60, binding.motion120};
        this.eventSource = eventSource;
        this.mpv = eventSource instanceof MpvPlayer mpvPlayer ? mpvPlayer : null;
        this.exo = exo;
    }

    void bind() {
        boolean available = hasController();
        binding.getRoot().setVisibility(available ? View.VISIBLE : View.GONE);
        if (!available) return;
        bindOptions();
        if (mpv != null) mpv.addMotionInterpolationListener(motionListener);
        else exo.addListener(motionListener);
        updateMotion();
    }

    private void bindOptions() {
        String[] labels = ResUtil.getStringArray(R.array.select_motion);
        for (int i = 0; i < options.length; i++) {
            int index = i;
            options[i].setText(labels[i]);
            options[i].setOnClickListener(view -> applyMotion(index));
        }
    }

    void release() {
        released = true;
        for (Chip option : options) option.setOnClickListener(null);
        if (mpv != null) mpv.removeMotionInterpolationListener(motionListener);
        else if (exo != null) exo.removeListener(motionListener);
    }

    private void applyMotion(int index) {
        int fps = PlayerSetting.getMotionFps(index);
        if (!canChangeMotion() || fps == getTargetFps()) {
            updateMotion();
            return;
        }
        pendingFps = fps;
        updateMotion();
        ListenableFuture<Void> update = mpv != null ? mpv.setMotionInterpolationFps(fps) : exo.setTargetFps(fps);
        Futures.addCallback(update, Task.callback(result -> onApplied(index), this::onFailed), MoreExecutors.directExecutor());
    }

    private void onApplied(int index) {
        pendingFps = null;
        PlayerSetting.putMotion(index);
        updateMotion();
    }

    private void onFailed(Throwable error) {
        pendingFps = null;
        if (!(error instanceof CancellationException)) Notify.show(Notify.getError(R.string.player_motion_failed, error));
        updateMotion();
    }

    void updateMotion() {
        if (released || !hasController()) return;
        boolean supported = canSetTarget();
        boolean pending = isUpdatePending();
        if (!supported && !pending && !isVideoOutputReady()) {
            checkOption(PlayerSetting.getMotion());
            updateStatus("");
            return;
        }
        int fps = pendingFps == null ? getTargetFps() : pendingFps;
        int index = getOptionIndex(fps);
        checkOption(index);
        for (int i = 0; i < options.length; i++) options[i].setEnabled(pending ? i == index : supported);
        updateStatus(getStatus(supported, pending, fps, index));
    }

    private void checkOption(int index) {
        int checkedId = index < 0 ? View.NO_ID : options[index].getId();
        if (binding.motionGroup.getCheckedChipId() == checkedId) return;
        if (checkedId == View.NO_ID) binding.motionGroup.clearCheck();
        else binding.motionGroup.check(checkedId);
    }

    private int getOptionIndex(int fps) {
        for (int i = 0; i < options.length; i++) if (PlayerSetting.getMotionFps(i) == fps) return i;
        return -1;
    }

    private boolean hasController() {
        return mpv != null || exo != null;
    }

    private boolean canChangeMotion() {
        return !released && !isUpdatePending() && canSetTarget();
    }

    private boolean canSetTarget() {
        return mpv != null ? mpv.canSetMotionInterpolationFps() : exo != null && exo.isSupported();
    }

    private boolean isUpdatePending() {
        return pendingFps != null || (mpv != null ? mpv.isMotionInterpolationUpdatePending() : exo != null && exo.isUpdatePending());
    }

    private int getTargetFps() {
        return mpv != null ? mpv.getMotionInterpolationFps() : exo.getTargetFps();
    }

    private String formatRate(int fps) {
        return ResUtil.getString(R.string.player_motion_fps, fps);
    }

    private String getStatus(boolean supported, boolean pending, int fps, int index) {
        if (!supported) return pending ? "" : ResUtil.getString(R.string.player_motion_unavailable);
        if (mpv != null && pendingFps == null && fps > 0) {
            float effectiveFps = mpv.getMotionInterpolationEffectiveTargetFps();
            if (effectiveFps > 0 && effectiveFps < fps - 0.001f) return ResUtil.getString(R.string.player_motion_capped, effectiveFps);
        }
        return index < 0 ? formatRate(fps) : "";
    }

    private boolean isVideoOutputReady() {
        return SettingPanelViews.isPlaybackPrepared(eventSource) && (mpv == null || mpv.getVideoEffectsSupport() != MpvPlayer.VIDEO_EFFECTS_UNAVAILABLE);
    }

    private void updateStatus(String text) {
        if (!TextUtils.equals(binding.motionStatus.getText(), text)) binding.motionStatus.setText(text);
        binding.motionStatus.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
