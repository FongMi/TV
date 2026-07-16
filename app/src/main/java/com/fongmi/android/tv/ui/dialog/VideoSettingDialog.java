package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogVideoSettingBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

public final class VideoSettingDialog {

    private PlayerManager player;

    public static VideoSettingDialog create() {
        return new VideoSettingDialog();
    }

    private static DialogVideoSettingBinding inflate(LayoutInflater inflater, ViewGroup container) {
        return DialogVideoSettingBinding.inflate(inflater, container, false);
    }

    public VideoSettingDialog player(PlayerManager player) {
        this.player = player;
        return this;
    }

    public void show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        for (Fragment fragment : manager.getFragments()) if (fragment instanceof BottomSheet || fragment instanceof SideSheet) return;
        if (Util.isFullscreenLand(activity) || Util.isLeanback()) new SideSheet(player).show(manager, null);
        else new BottomSheet(player).show(manager, null);
    }

    public static final class BottomSheet extends BaseBottomSheetDialog {

        private final PlayerManager player;
        private DialogVideoSettingBinding binding;
        private VideoSettingPanel panel;

        BottomSheet(PlayerManager player) {
            this.player = player;
        }

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = VideoSettingDialog.inflate(inflater, container);
        }

        @Override
        protected int getMaxHeight() {
            return ResUtil.getScreenHeight() / 2;
        }

        @Override
        protected void initView() {
            panel = new VideoSettingPanel(binding, player);
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

        private final PlayerManager player;
        private DialogVideoSettingBinding binding;
        private VideoSettingPanel panel;

        SideSheet(PlayerManager player) {
            this.player = player;
        }

        @Override
        protected int getWidth() {
            return Math.min(ResUtil.dp2px(420), ResUtil.getScreenWidth() / 2);
        }

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = VideoSettingDialog.inflate(inflater, container);
        }

        @Override
        protected void initView() {
            panel = new VideoSettingPanel(binding, player);
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
