package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogBufferBinding;
import com.fongmi.android.tv.impl.BufferCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class BufferDialog {

    private final DialogBufferBinding binding;
    private final BufferCallback callback;
    private int value;

    public static BufferDialog create(Fragment fragment) {
        return new BufferDialog(fragment);
    }

    public BufferDialog(Fragment fragment) {
        this.callback = (BufferCallback) fragment;
        this.binding = DialogBufferBinding.inflate(LayoutInflater.from(fragment.getContext()));
    }

    public void show() {
        initDialog();
        initView();
    }

    private void initDialog() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.player_exo_buffer).setView(binding.getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.slider.setValue(value = Setting.getBuffer());
    }

    private void onPositive(DialogInterface dialog, int which) {
        callback.setBuffer((int) binding.slider.getValue());
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        callback.setBuffer(value);
        dialog.dismiss();
    }
}
