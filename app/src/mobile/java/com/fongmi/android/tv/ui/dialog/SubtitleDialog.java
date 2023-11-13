package com.fongmi.android.tv.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogSubtitleBinding;
import com.fongmi.android.tv.impl.SubtitleCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SubtitleDialog {

    private final DialogSubtitleBinding binding;
    private final SubtitleCallback callback;
    private boolean listen;
    private int value;

    public static SubtitleDialog create(Fragment fragment) {
        return new SubtitleDialog(fragment).listen(false);
    }

    public static SubtitleDialog create(Context context) {
        return new SubtitleDialog(context).listen(true);
    }

    public SubtitleDialog(Fragment fragment) {
        this.callback = (SubtitleCallback) fragment;
        this.binding = DialogSubtitleBinding.inflate(LayoutInflater.from(fragment.getContext()));
    }

    public SubtitleDialog(Context context) {
        this.callback = (SubtitleCallback) context;
        this.binding = DialogSubtitleBinding.inflate(LayoutInflater.from(context));
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
        AlertDialog dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.setting_player_subtitle).setView(binding.getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.slider.setValue(value = Setting.getSubtitle());
    }

    private void initEvent() {
        if (listen) binding.slider.addOnChangeListener((slider, value, fromUser) -> callback.setSubtitle((int) value));
    }

    private void onPositive(DialogInterface dialog, int which) {
        callback.setSubtitle((int) binding.slider.getValue());
        Setting.putSubtitle((int) binding.slider.getValue());
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        callback.setSubtitle(value);
        dialog.dismiss();
    }
}
