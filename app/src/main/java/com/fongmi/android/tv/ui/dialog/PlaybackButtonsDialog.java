package com.fongmi.android.tv.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogMpvScriptButtonBinding;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;

public final class PlaybackButtonsDialog {

    private static final String TAG = "playback_buttons";

    public static void show(FragmentActivity activity, String id) {
        FragmentManager manager = activity.getSupportFragmentManager();
        if (manager.isStateSaved() || manager.findFragmentByTag(TAG) != null) return;
        DialogFragment dialog = Util.isFullscreenLand(activity) || Util.isLeanback() ? new SideSheet() : new BottomSheet();
        Bundle args = new Bundle();
        args.putString("selected", id);
        dialog.setArguments(args);
        dialog.showNow(manager, TAG);
    }

    public static final class BottomSheet extends BaseBottomSheetDialog {

        private final PlaybackButtonsPanel panel = new PlaybackButtonsPanel(this);
        private DialogMpvScriptButtonBinding binding;

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = DialogMpvScriptButtonBinding.inflate(inflater, container, false);
        }

        @Override
        protected int getMaxHeight() {
            return ResUtil.getScreenHeight() / 2;
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            panel.bind(binding, savedInstanceState);
        }

        @Override
        public void onStart() {
            super.onStart();
            panel.start();
        }

        @Override
        public void onStop() {
            panel.stop();
            super.onStop();
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle state) {
            super.onSaveInstanceState(state);
            panel.save(state);
        }

        @Override
        public void onDestroyView() {
            panel.release();
            binding = null;
            super.onDestroyView();
        }
    }

    public static final class SideSheet extends BaseSideSheetDialog {

        private final PlaybackButtonsPanel panel = new PlaybackButtonsPanel(this);
        private DialogMpvScriptButtonBinding binding;

        @Override
        protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
            return binding = DialogMpvScriptButtonBinding.inflate(inflater, container, false);
        }

        @Override
        protected int getWidth() {
            return Math.min(ResUtil.dp2px(420), ResUtil.getScreenWidth() / 2);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            panel.bind(binding, savedInstanceState);
        }

        @Override
        public void onStart() {
            super.onStart();
            panel.start();
        }

        @Override
        public void onStop() {
            panel.stop();
            super.onStop();
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle state) {
            super.onSaveInstanceState(state);
            panel.save(state);
        }

        @Override
        public void onDestroyView() {
            panel.release();
            binding = null;
            super.onDestroyView();
        }
    }
}
