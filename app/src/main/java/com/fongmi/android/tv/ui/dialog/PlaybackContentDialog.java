package com.fongmi.android.tv.ui.dialog;

import android.content.res.Configuration;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogPlaybackContentBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.ui.activity.PlaybackActivity;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

import java.util.Objects;

public final class PlaybackContentDialog {

    public static final int SUBTITLE = 0;
    public static final int DANMAKU = 1;

    private int type;

    public static PlaybackContentDialog create() {
        return new PlaybackContentDialog();
    }

    public PlaybackContentDialog type(int type) {
        this.type = type;
        return this;
    }

    public void show(FragmentActivity activity) {
        if (!(activity instanceof PlaybackActivity)) return;
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved()) return;
        for (Fragment fragment : manager.getFragments()) if (fragment instanceof BottomSheet || fragment instanceof SideSheet) return;
        State state = new ViewModelProvider(activity).get(State.class);
        state.tab = type;
        showSheet(activity);
    }

    private static boolean isSideSheet(FragmentActivity activity) {
        return Util.isLeanback() || ResUtil.isLand(activity);
    }

    private static void showSheet(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved()) return;
        for (Fragment fragment : manager.getFragments()) if (fragment instanceof BottomSheet || fragment instanceof SideSheet) return;
        if (isSideSheet(activity)) new SideSheet().show(manager, null);
        else new BottomSheet().show(manager, null);
    }

    private static void updateSheet(DialogFragment fragment) {
        if (!fragment.isAdded() || fragment.getView() == null) return;
        fragment.getView().post(() -> {
            if (!fragment.isAdded() || fragment.getParentFragmentManager().isStateSaved()) return;
            FragmentActivity activity = fragment.requireActivity();
            if ((fragment instanceof SideSheet) == isSideSheet(activity)) return;
            fragment.dismissNow();
            showSheet(activity);
        });
    }

    public static final class BottomSheet extends BaseBottomSheetDialog {

        private DialogPlaybackContentBinding binding;
        private PlaybackContentPanel panel;

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = DialogPlaybackContentBinding.inflate(inflater, container, false);
        }

        @Override
        protected int getMaxHeight() {
            return ResUtil.getScreenHeight() / 2;
        }

        @Override
        protected void initView() {
            panel = new PlaybackContentPanel(binding, (PlaybackActivity) requireActivity());
            panel.bind();
        }

        @Override
        public void onResume() {
            super.onResume();
            if (panel != null) panel.start();
            updateSheet(this);
        }

        @Override
        public void onPause() {
            if (panel != null) panel.stop();
            super.onPause();
        }

        @Override
        public void onConfigurationChanged(@NonNull Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            updateSheet(this);
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

        private DialogPlaybackContentBinding binding;
        private PlaybackContentPanel panel;

        @Override
        protected int getWidth() {
            return Math.min(ResUtil.dp2px(420), ResUtil.getScreenWidth() / 2);
        }

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = DialogPlaybackContentBinding.inflate(inflater, container, false);
        }

        @Override
        protected void initView() {
            panel = new PlaybackContentPanel(binding, (PlaybackActivity) requireActivity());
            panel.bind();
        }

        @Override
        public void onResume() {
            super.onResume();
            if (panel != null) panel.start();
            updateSheet(this);
        }

        @Override
        public void onPause() {
            if (panel != null) panel.stop();
            super.onPause();
        }

        @Override
        public void onConfigurationChanged(@NonNull Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            updateSheet(this);
        }

        @Override
        public void onDestroyView() {
            if (panel != null) panel.release();
            panel = null;
            binding = null;
            super.onDestroyView();
        }
    }

    public static final class State extends ViewModel {

        final Parcelable[] scroll = new Parcelable[2];
        final String[] keyword = {"", ""};
        final boolean[] follow = {true, true};
        boolean hintShown;
        String media;
        int tab;

        void prepare(PlayerManager player) {
            String current = player.getKey() + "\n" + player.getUrl();
            if (Objects.equals(media, current)) return;
            media = current;
            for (int i = 0; i < scroll.length; i++) {
                scroll[i] = null;
                keyword[i] = "";
                follow[i] = true;
            }
        }
    }
}
