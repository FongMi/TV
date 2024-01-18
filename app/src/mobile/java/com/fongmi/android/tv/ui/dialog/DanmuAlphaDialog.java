package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogDanmuAlphaBinding;
import com.fongmi.android.tv.impl.DanmuAlphaCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DanmuAlphaDialog {

    private final DialogDanmuAlphaBinding binding;
    private final DanmuAlphaCallback callback;
    private int value;

    public static DanmuAlphaDialog create(Fragment fragment) {
        return new DanmuAlphaDialog(fragment);
    }

    public DanmuAlphaDialog(Fragment fragment) {
        this.callback = (DanmuAlphaCallback) fragment;
        this.binding = DialogDanmuAlphaBinding.inflate(LayoutInflater.from(fragment.getContext()));
    }

    public void show() {
        initDialog();
        initView();
    }

    private void initDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.player_danmu_alpha).setView(binding.getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.slider.setValue(value = Setting.getDanmuAlpha());
    }

    private void onPositive(DialogInterface dialog, int which) {
        callback.setDanmuAlpha((int) binding.slider.getValue());
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        callback.setDanmuAlpha(value);
        dialog.dismiss();
    }
}
