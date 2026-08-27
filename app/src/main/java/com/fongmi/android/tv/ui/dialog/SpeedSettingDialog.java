package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogSpeedSettingBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

public final class SpeedSettingDialog {

    private PlayerManager player;
    private boolean save;

    public static SpeedSettingDialog create() {
        return new SpeedSettingDialog();
    }

    private static DialogSpeedSettingBinding inflate(LayoutInflater inflater, ViewGroup container) {
        return DialogSpeedSettingBinding.inflate(inflater, container, false);
    }

    public SpeedSettingDialog player(PlayerManager player) {
        this.player = player;
        return this;
    }

    public SpeedSettingDialog save(boolean save) {
        this.save = save;
        return this;
    }

    public void show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        for (Fragment fragment : manager.getFragments()) if (fragment instanceof BottomSheet || fragment instanceof SideSheet) return;
        if (Util.isFullscreenLand(activity) || Util.isLeanback()) new SideSheet(player, save).show(manager, null);
        else new BottomSheet(player, save).show(manager, null);
    }

    public static final class BottomSheet extends BaseBottomSheetDialog {

        private final PlayerManager player;
        private final boolean save;
        private DialogSpeedSettingBinding binding;
        private SpeedSettingPanel panel;

        BottomSheet(PlayerManager player, boolean save) {
            this.player = player;
            this.save = save;
        }

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = SpeedSettingDialog.inflate(inflater, container);
        }

        @Override
        protected int getMaxHeight() {
            return ResUtil.getScreenHeight() / 2;
        }

        @Override
        protected void initView() {
            panel = new SpeedSettingPanel(binding, player, save);
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
        private final boolean save;
        private DialogSpeedSettingBinding binding;
        private SpeedSettingPanel panel;

        SideSheet(PlayerManager player, boolean save) {
            this.player = player;
            this.save = save;
        }

        @Override
        protected int getWidth() {
            return Math.min(ResUtil.dp2px(440), ResUtil.getScreenWidth() / 3 + ResUtil.dp2px(20));
        }

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = SpeedSettingDialog.inflate(inflater, container);
        }

        @Override
        protected void initView() {
            panel = new SpeedSettingPanel(binding, player, save);
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
