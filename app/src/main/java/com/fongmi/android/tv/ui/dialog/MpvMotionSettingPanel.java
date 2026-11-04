package com.fongmi.android.tv.ui.dialog;

import android.view.View;

import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.mpvplayer.MpvPlayer;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ViewMpvMotionBinding;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.CancellationException;

final class MpvMotionSettingPanel {

    private static final int[] FRAME_RATES = {0, 60, 120};

    private final ViewMpvMotionBinding binding;
    @Nullable
    private final MpvPlayer player;
    private final Runnable motionListener = this::updateMotion;
    private boolean released;

    MpvMotionSettingPanel(ViewMpvMotionBinding binding, @Nullable Player player) {
        this.binding = binding;
        this.player = player instanceof MpvPlayer mpv ? mpv : null;
    }

    void bind() {
        binding.getRoot().setVisibility(player == null ? View.GONE : View.VISIBLE);
        if (player == null) return;
        String[] labels = ResUtil.getStringArray(R.array.select_mpv_motion);
        MaterialButton[] modes = getModes();
        for (int i = 0; i < modes.length; i++) bindMode(modes[i], labels[i], i);
        player.addMotionInterpolationListener(motionListener);
        updateMotion();
    }

    void release() {
        released = true;
        for (MaterialButton mode : getModes()) mode.setOnClickListener(null);
        if (player != null) player.removeMotionInterpolationListener(motionListener);
    }

    private void bindMode(MaterialButton button, String label, int index) {
        button.setText(label);
        button.setToggleCheckedStateOnClick(false);
        button.setOnClickListener(view -> selectMode(index));
    }

    private void selectMode(int index) {
        if (player == null || !player.isMotionInterpolationSupported() || player.isMotionInterpolationUpdatePending()) return;
        Futures.addCallback(player.setMotionInterpolationFps(FRAME_RATES[index]), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                onApplied(index);
            }

            @Override
            public void onFailure(Throwable error) {
                onFailed(error);
            }
        }, MoreExecutors.directExecutor());
    }

    private void onApplied(int index) {
        PlayerSetting.putMpvMotion(index);
        updateMotion();
    }

    private void onFailed(Throwable error) {
        if (!(error instanceof CancellationException)) Notify.show(Notify.getError(R.string.player_mpv_motion_failed, error));
        updateMotion();
    }

    private void updateMotion() {
        if (released || player == null) return;
        int fps = player.getMotionInterpolationFps();
        boolean supported = player.isMotionInterpolationSupported();
        SettingPanelViews.applyEnabled(binding.modes, supported);
        if (supported) updateChecked(fps);
        binding.status.setText(getStatusText(supported, player.isMotionInterpolationUpdatePending(), fps, player.getMotionInterpolationEffectiveTargetFps()));
    }

    private void updateChecked(int fps) {
        int checked = View.NO_ID;
        MaterialButton[] modes = getModes();
        for (int i = 0; i < modes.length; i++) if (fps == FRAME_RATES[i]) checked = modes[i].getId();
        if (checked == View.NO_ID) binding.modes.clearChecked();
        else binding.modes.check(checked);
    }

    private String getStatusText(boolean supported, boolean pending, int fps, float effective) {
        if (!supported) return ResUtil.getString(R.string.player_mpv_motion_unavailable);
        if (pending) return ResUtil.getString(R.string.player_mpv_motion_applying);
        if (effective > 0 && effective < fps - 0.001f) return ResUtil.getString(R.string.player_mpv_motion_capped, effective);
        return fps == 0 ? ResUtil.getString(R.string.setting_off) : ResUtil.getString(R.string.player_mpv_motion_target, fps);
    }

    private MaterialButton[] getModes() {
        return new MaterialButton[]{binding.mode0, binding.mode60, binding.mode120};
    }
}
