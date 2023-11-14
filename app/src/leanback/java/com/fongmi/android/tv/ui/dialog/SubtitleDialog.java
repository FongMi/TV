package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Dimension;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.text.Cue;

import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogSubtitleBinding;
import com.fongmi.android.tv.impl.SubtitleCallback;
import com.fongmi.android.tv.player.ExoUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;

public class SubtitleDialog {

    private final DialogSubtitleBinding binding;
    private final SubtitleCallback callback;
    private final AlertDialog dialog;
    private boolean listen;
    private int value;

    public static SubtitleDialog create(FragmentActivity activity) {
        return new SubtitleDialog(activity);
    }

    public SubtitleDialog(FragmentActivity activity) {
        this.callback = (SubtitleCallback) activity;
        this.binding = DialogSubtitleBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
    }

    public SubtitleDialog listen(boolean listen) {
        this.listen = listen;
        return this;
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * 0.45f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.slider.setValue(Setting.getSubtitle());
        binding.preview.setStyle(ExoUtil.getCaptionStyle());
        binding.preview.setVisibility(listen ? View.GONE : View.VISIBLE);
        binding.preview.setFixedTextSize(Dimension.SP, value = Setting.getSubtitle());
        binding.preview.setCues(Arrays.asList(new Cue.Builder().setText("影視天下第一").build()));
    }

    private void initEvent() {
        binding.positive.setOnClickListener(this::onPositive);
        binding.negative.setOnClickListener(this::onNegative);
        if (listen) binding.slider.addOnChangeListener((slider, value, fromUser) -> callback.setSubtitle((int) value));
        else binding.slider.addOnChangeListener((slider, value, fromUser) -> binding.preview.setFixedTextSize(Dimension.SP, value));
    }

    private void onPositive(View view) {
        callback.setSubtitle((int) binding.slider.getValue());
        Setting.putSubtitle((int) binding.slider.getValue());
        dialog.dismiss();
    }

    private void onNegative(View view) {
        callback.setSubtitle(value);
        dialog.dismiss();
    }
}
