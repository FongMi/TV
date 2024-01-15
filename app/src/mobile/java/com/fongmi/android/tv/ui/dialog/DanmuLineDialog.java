package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogDanmuLineBinding;
import com.fongmi.android.tv.impl.DanmuLineCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DanmuLineDialog {

    private final DialogDanmuLineBinding binding;
    private final DanmuLineCallback callback;
    private int value;

    public static DanmuLineDialog create(Fragment fragment) {
        return new DanmuLineDialog(fragment);
    }

    public DanmuLineDialog(Fragment fragment) {
        this.callback = (DanmuLineCallback) fragment;
        this.binding = DialogDanmuLineBinding.inflate(LayoutInflater.from(fragment.getContext()));
    }

    public void show() {
        initDialog();
        initView();
    }

    private void initDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.player_danmu_line).setView(binding.getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.slider.setValue(value = Setting.getDanmuLine(2));
    }

    private void onPositive(DialogInterface dialog, int which) {
        callback.setDanmuLine((int) binding.slider.getValue());
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        callback.setDanmuLine(value);
        dialog.dismiss();
    }
}
