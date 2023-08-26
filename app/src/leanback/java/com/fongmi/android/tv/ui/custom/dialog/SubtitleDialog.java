package com.fongmi.android.tv.ui.custom.dialog;

import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogSubtitleBinding;
import com.fongmi.android.tv.impl.SubtitleCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SubtitleDialog {

    private final DialogSubtitleBinding binding;
    private final SubtitleCallback callback;
    private final AlertDialog dialog;
    private int value;

    public static SubtitleDialog create(FragmentActivity activity) {
        return new SubtitleDialog(activity);
    }

    public SubtitleDialog(FragmentActivity activity) {
        this.callback = (SubtitleCallback) activity;
        this.binding = DialogSubtitleBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.slider.setValue(value = Setting.getSubtitle());
    }

    private void initEvent() {
        binding.positive.setOnClickListener(this::onPositive);
        binding.negative.setOnClickListener(this::onNegative);
        binding.slider.addOnChangeListener((slider, value, fromUser) -> callback.setSubtitle((int) value));
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
