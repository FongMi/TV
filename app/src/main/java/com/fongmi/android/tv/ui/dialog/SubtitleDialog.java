package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.ui.SubtitleView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogSubtitleBinding;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.bassaer.library.MDColor;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public final class SubtitleDialog extends BaseDialog {

    private DialogSubtitleBinding binding;
    private SubtitleView subtitleView;
    private boolean full;

    public static SubtitleDialog create() {
        return new SubtitleDialog();
    }

    public SubtitleDialog view(SubtitleView subtitleView) {
        this.subtitleView = subtitleView;
        return this;
    }

    public SubtitleDialog full(boolean full) {
        this.full = full;
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected boolean transparent() {
        return full;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogSubtitleBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        int count = binding.getRoot().getChildCount();
        if (full) for (int i = 0; i < count; i++) ((ImageView) binding.getRoot().getChildAt(i)).getDrawable().setTint(MDColor.WHITE);
    }

    @Override
    protected void initEvent() {
        //binding.up.setOnClickListener(this::onUp);
        //binding.down.setOnClickListener(this::onDown);
        //binding.large.setOnClickListener(this::onLarge);
        //binding.small.setOnClickListener(this::onSmall);
        binding.reset.setOnClickListener(this::onReset);
    }

    /*private void onUp(View view) {
        subtitleView.addPosition(0.005f);
        Setting.putSubtitlePosition(subtitleView.getPosition());
    }

    private void onDown(View view) {
        subtitleView.subPosition(0.005f);
        Setting.putSubtitlePosition(subtitleView.getPosition());
    }

    private void onLarge(View view) {
        subtitleView.addTextSize(0.002f);
        Setting.putSubtitleTextSize(subtitleView.getTextSize());
    }

    private void onSmall(View view) {
        subtitleView.subTextSize(0.002f);
        Setting.putSubtitleTextSize(subtitleView.getTextSize());
    }*/

    private void onReset(View view) {
        Setting.putSubtitleTextSize(0.0f);
        Setting.putSubtitlePosition(0.0f);
        //subtitleView.setBottomPosition(0.0f);
        subtitleView.setUserDefaultTextSize();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (full) setDimAmount(0.5f);
        getDialog().getWindow().setLayout(ResUtil.dp2px(full ? 232 : 216), -1);
    }
}
