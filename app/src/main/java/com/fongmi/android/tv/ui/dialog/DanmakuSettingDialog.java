package com.fongmi.android.tv.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogDanmakuSettingBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.subtitle.ExternalFont;
import com.fongmi.android.tv.ui.activity.PlaybackActivity;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

public final class DanmakuSettingDialog {

    public static DanmakuSettingDialog create() {
        return new DanmakuSettingDialog();
    }

    private int tab;

    public DanmakuSettingDialog tab(int tab) {
        this.tab = tab;
        return this;
    }

    public void show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved()) return;
        for (Fragment fragment : manager.getFragments()) if (fragment instanceof BottomSheet || fragment instanceof SideSheet) return;
        if (Util.isFullscreenLand(activity) || Util.isLeanback()) SideSheet.newInstance(tab).show(manager, null);
        else BottomSheet.newInstance(tab).show(manager, null);
    }

    public static final class BottomSheet extends BaseBottomSheetDialog {

        static BottomSheet newInstance(int tab) {
            BottomSheet sheet = new BottomSheet();
            Bundle args = new Bundle();
            args.putInt("tab", tab);
            sheet.setArguments(args);
            return sheet;
        }

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
            PlaybackDialogFocus.preferCheckedChips(binding.getRoot());
            if (requireActivity() instanceof PlaybackActivity) PlaybackDialog.observe(this, this::bindPlayer);
            else bindPlayer(null);
        }

        private void bindPlayer(PlayerManager player) {
            if (panel != null) panel.release();
            fontSelector.release();
            panel = null;
            if (player == null && requireActivity() instanceof PlaybackActivity) return;
            panel = new DanmakuSettingPanel(binding, player, fontSelector, requireArguments().getInt("tab"));
            panel.bind();
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

        static SideSheet newInstance(int tab) {
            SideSheet sheet = new SideSheet();
            Bundle args = new Bundle();
            args.putInt("tab", tab);
            sheet.setArguments(args);
            return sheet;
        }

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
            PlaybackDialogFocus.preferCheckedChips(binding.getRoot());
            if (requireActivity() instanceof PlaybackActivity) PlaybackDialog.observe(this, this::bindPlayer);
            else bindPlayer(null);
        }

        private void bindPlayer(PlayerManager player) {
            if (panel != null) panel.release();
            fontSelector.release();
            panel = null;
            if (player == null && requireActivity() instanceof PlaybackActivity) return;
            panel = new DanmakuSettingPanel(binding, player, fontSelector, requireArguments().getInt("tab"));
            panel.bind();
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
