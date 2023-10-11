package com.fongmi.android.tv.ui.custom.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogBufferBinding;
import com.fongmi.android.tv.impl.BufferCallback;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class BufferDialog {

    private final DialogBufferBinding binding;
    private final BufferCallback callback;
    private final AlertDialog dialog;
    private int value;

    public static BufferDialog create(FragmentActivity activity) {
        return new BufferDialog(activity);
    }

    public BufferDialog(FragmentActivity activity) {
        this.callback = (BufferCallback) activity;
        this.binding = DialogBufferBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
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
        binding.slider.setValue(value = Setting.getBuffer());
    }

    private void initEvent() {
        binding.positive.setOnClickListener(this::onPositive);
        binding.negative.setOnClickListener(this::onNegative);
    }

    private void onPositive(View view) {
        callback.setBuffer((int) binding.slider.getValue());
        Setting.putBuffer((int) binding.slider.getValue());
        dialog.dismiss();
    }

    private void onNegative(View view) {
        callback.setBuffer(value);
        dialog.dismiss();
    }
}
