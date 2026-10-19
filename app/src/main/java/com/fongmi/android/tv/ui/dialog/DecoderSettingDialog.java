package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogDecoderModeBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

public final class DecoderSettingDialog {

    public static DecoderSettingDialog create() {
        return new DecoderSettingDialog();
    }

    public void show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved()) return;
        for (Fragment fragment : manager.getFragments()) if (fragment instanceof BottomSheet || fragment instanceof SideSheet) return;
        if (Util.isFullscreenLand(activity) || Util.isLeanback()) new SideSheet().show(manager, null);
        else new BottomSheet().show(manager, null);
    }

    public static final class BottomSheet extends BaseBottomSheetDialog {

        private DialogDecoderModeBinding binding;
        private DecoderSettingPanel panel;

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = DialogDecoderModeBinding.inflate(inflater, container, false);
        }

        @Override
        protected int getMaxHeight() {
            return ResUtil.getScreenHeight() / 2;
        }

        @Override
        protected void initView() {
            PlaybackDialog.observe(this, this::bindPlayer);
        }

        private void bindPlayer(PlayerManager player) {
            if (panel != null) panel.release();
            panel = null;
            if (player == null) return;
            panel = new DecoderSettingPanel(binding, player);
            panel.bind();
        }

        @Override
        public void onDestroyView() {
            if (panel != null) panel.release();
            panel = null;
            binding = null;
            super.onDestroyView();
        }
    }

    public static final class SideSheet extends BaseSideSheetDialog {

        private DialogDecoderModeBinding binding;
        private DecoderSettingPanel panel;

        @Override
        protected int getWidth() {
            return Math.min(ResUtil.dp2px(420), ResUtil.getScreenWidth() / 2);
        }

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = DialogDecoderModeBinding.inflate(inflater, container, false);
        }

        @Override
        protected void initView() {
            PlaybackDialog.observe(this, this::bindPlayer);
        }

        private void bindPlayer(PlayerManager player) {
            if (panel != null) panel.release();
            panel = null;
            if (player == null) return;
            panel = new DecoderSettingPanel(binding, player);
            panel.bind();
        }

        @Override
        public void onDestroyView() {
            if (panel != null) panel.release();
            panel = null;
            binding = null;
            super.onDestroyView();
        }
    }
}
