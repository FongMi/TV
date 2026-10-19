package com.fongmi.android.tv.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogSpeedSettingBinding;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

public final class SpeedSettingDialog {

    private boolean save;

    public static SpeedSettingDialog create() {
        return new SpeedSettingDialog();
    }

    public SpeedSettingDialog save(boolean save) {
        this.save = save;
        return this;
    }

    public void show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved()) return;
        for (Fragment fragment : manager.getFragments()) if (fragment instanceof BottomSheet || fragment instanceof SideSheet) return;
        DialogFragment dialog = Util.isFullscreenLand(activity) || Util.isLeanback() ? new SideSheet() : new BottomSheet();
        Bundle arguments = new Bundle();
        arguments.putBoolean("save", save);
        dialog.setArguments(arguments);
        dialog.show(manager, null);
    }

    public static final class BottomSheet extends BaseBottomSheetDialog {

        private DialogSpeedSettingBinding binding;

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = DialogSpeedSettingBinding.inflate(inflater, container, false);
        }

        @Override
        protected int getMaxHeight() {
            return ResUtil.getScreenHeight() / 2;
        }

        @Override
        protected void initView() {
            PlaybackDialog.observe(this, player -> {
                if (player != null) new SpeedSettingPanel(binding, player, requireArguments().getBoolean("save")).bind();
            });
        }

        @Override
        public void onDestroyView() {
            binding = null;
            super.onDestroyView();
        }
    }

    public static final class SideSheet extends BaseSideSheetDialog {

        private DialogSpeedSettingBinding binding;

        @Override
        protected int getWidth() {
            return Math.min(ResUtil.dp2px(440), ResUtil.getScreenWidth() / 3 + ResUtil.dp2px(20));
        }

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = DialogSpeedSettingBinding.inflate(inflater, container, false);
        }

        @Override
        protected void initView() {
            PlaybackDialog.observe(this, player -> {
                if (player != null) new SpeedSettingPanel(binding, player, requireArguments().getBoolean("save")).bind();
            });
        }

        @Override
        public void onDestroyView() {
            binding = null;
            super.onDestroyView();
        }
    }
}
