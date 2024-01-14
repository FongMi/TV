package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogDanmuMaxlineBinding;
import com.fongmi.android.tv.impl.DanmuMaxLineCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DanmuMaxLineDialog {

    private final DialogDanmuMaxlineBinding binding;
    private final DanmuMaxLineCallback callback;
    private int value;

    public static DanmuMaxLineDialog create(Fragment fragment) {
        return new DanmuMaxLineDialog(fragment);
    }

    public DanmuMaxLineDialog(Fragment fragment) {
        this.callback = (DanmuMaxLineCallback) fragment;
        this.binding = DialogDanmuMaxlineBinding.inflate(LayoutInflater.from(fragment.getContext()));
    }

    public void show() {
        initDialog();
        initView();
    }

    private void initDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.setting_danmu_maxline).setView(binding.getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.slider.setValue(value = Setting.getDanmuMaxLine(2));
    }

    private void onPositive(DialogInterface dialog, int which) {
        callback.setDanmuMaxLine((int) binding.slider.getValue());
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        callback.setDanmuMaxLine(value);
        dialog.dismiss();
    }
}
