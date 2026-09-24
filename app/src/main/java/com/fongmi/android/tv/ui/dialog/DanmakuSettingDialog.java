package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogDanmakuSettingBinding;
import com.fongmi.android.tv.player.subtitle.ExternalFont;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

public final class DanmakuSettingDialog {

    public static DanmakuSettingDialog create() {
        return new DanmakuSettingDialog();
    }

    public void show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved()) return;
        for (Fragment fragment : manager.getFragments()) if (fragment instanceof BottomSheet || fragment instanceof SideSheet) return;
        if (Util.isFullscreenLand(activity) || Util.isLeanback()) new SideSheet().show(manager, null);
        else new BottomSheet().show(manager, null);
    }

    public static final class BottomSheet extends BaseBottomSheetDialog {

        private final ExternalFontSelector fontSelector = new ExternalFontSelector(this, this::onFontSelected);
        private DialogDanmakuSettingBinding binding;
        private DanmakuSettingPanel panel;

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = DialogDanmakuSettingBinding.inflate(inflater, container, false);
        }

        @Override
        protected int getMaxHeight() {
            return ResUtil.getScreenHeight() / 2;
        }

        @Override
        protected void initView() {
            PlaybackDialog.observe(this, player -> {
                if (panel != null) panel.release();
                fontSelector.release();
                panel = null;
                if (player == null) return;
                panel = new DanmakuSettingPanel(binding, player, fontSelector);
                panel.bind();
            });
        }

        private void onFontSelected(@Nullable ExternalFont.Item font) {
            if (panel != null) panel.onFontSelected(font);
        }

        @Override
        public void onDestroyView() {
            if (panel != null) panel.release();
            fontSelector.release();
            panel = null;
            binding = null;
            super.onDestroyView();
        }
    }

    public static final class SideSheet extends BaseSideSheetDialog {

        private final ExternalFontSelector fontSelector = new ExternalFontSelector(this, this::onFontSelected);
        private DialogDanmakuSettingBinding binding;
        private DanmakuSettingPanel panel;

        @Override
        protected int getWidth() {
            return Math.min(ResUtil.dp2px(420), ResUtil.getScreenWidth() / 2);
        }

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = DialogDanmakuSettingBinding.inflate(inflater, container, false);
        }

        @Override
        protected void initView() {
            PlaybackDialog.observe(this, player -> {
                if (panel != null) panel.release();
                fontSelector.release();
                panel = null;
                if (player == null) return;
                panel = new DanmakuSettingPanel(binding, player, fontSelector);
                panel.bind();
            });
        }

        private void onFontSelected(@Nullable ExternalFont.Item font) {
            if (panel != null) panel.onFontSelected(font);
        }

        @Override
        public void onDestroyView() {
            if (panel != null) panel.release();
            fontSelector.release();
            panel = null;
            binding = null;
            super.onDestroyView();
        }
    }
}
