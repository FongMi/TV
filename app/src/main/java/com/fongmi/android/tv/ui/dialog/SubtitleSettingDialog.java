package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogSubtitleSettingBinding;
import com.fongmi.android.tv.player.subtitle.ExternalFont;
import com.fongmi.android.tv.ui.activity.PlaybackActivity;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

public final class SubtitleSettingDialog {

    public static SubtitleSettingDialog create() {
        return new SubtitleSettingDialog();
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
        private DialogSubtitleSettingBinding binding;
        private SubtitleSettingPanel panel;

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = DialogSubtitleSettingBinding.inflate(inflater, container, false);
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
                PlaybackActivity activity = (PlaybackActivity) requireActivity();
                panel = new SubtitleSettingPanel(binding, activity.getPlaybackSubtitleView(), player, fontSelector);
                panel.bind();
            });
        }

        private void onFontSelected(@Nullable ExternalFont.Item font) {
            if (panel != null) panel.onFontSelected(font);
        }

        @Override
        public void onResume() {
            super.onResume();
            if (panel != null) panel.onResume();
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
        private DialogSubtitleSettingBinding binding;
        private SubtitleSettingPanel panel;

        @Override
        protected int getWidth() {
            return Math.min(ResUtil.dp2px(420), ResUtil.getScreenWidth() / 2);
        }

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = DialogSubtitleSettingBinding.inflate(inflater, container, false);
        }

        @Override
        protected void initView() {
            PlaybackDialog.observe(this, player -> {
                if (panel != null) panel.release();
                fontSelector.release();
                panel = null;
                if (player == null) return;
                PlaybackActivity activity = (PlaybackActivity) requireActivity();
                panel = new SubtitleSettingPanel(binding, activity.getPlaybackSubtitleView(), player, fontSelector);
                panel.bind();
            });
        }

        private void onFontSelected(@Nullable ExternalFont.Item font) {
            if (panel != null) panel.onFontSelected(font);
        }

        @Override
        public void onResume() {
            super.onResume();
            if (panel != null) panel.onResume();
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
