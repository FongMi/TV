package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogDanmuSizeBinding;
import com.fongmi.android.tv.impl.DanmuSizeCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DanmuSizeDialog {

    private final DialogDanmuSizeBinding binding;
    private final DanmuSizeCallback callback;
    private float value;

    public static DanmuSizeDialog create(Fragment fragment) {
        return new DanmuSizeDialog(fragment);
    }

    public DanmuSizeDialog(Fragment fragment) {
        this.callback = (DanmuSizeCallback) fragment;
        this.binding = DialogDanmuSizeBinding.inflate(LayoutInflater.from(fragment.getContext()));
    }

    public void show() {
        initDialog();
        initView();
    }

    private void initDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.player_danmu_size).setView(binding.getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.slider.setValue(value = Setting.getDanmuSize());
    }

    private void onPositive(DialogInterface dialog, int which) {
        callback.setDanmuSize(binding.slider.getValue());
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        callback.setDanmuSize(value);
        dialog.dismiss();
    }
}
