package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogDanmuMaxlineBinding;
import com.fongmi.android.tv.impl.DanmuMaxLineCallback;
import com.fongmi.android.tv.utils.KeyUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DanmuMaxLineDialog {

    private final DialogDanmuMaxlineBinding binding;
    private final DanmuMaxLineCallback callback;
    private final AlertDialog dialog;

    public static DanmuMaxLineDialog create(FragmentActivity activity) {
        return new DanmuMaxLineDialog(activity);
    }

    public DanmuMaxLineDialog(FragmentActivity activity) {
        this.callback = (DanmuMaxLineCallback) activity;
        this.binding = DialogDanmuMaxlineBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void initView() {
        binding.slider.setValue(Setting.getDanmuMaxLine(3));
    }

    private void initEvent() {
        binding.slider.addOnChangeListener((slider, value, fromUser) -> callback.setDanmuMaxLine((int) value));
        binding.slider.setOnKeyListener((view, keyCode, event) -> {
            boolean enter = KeyUtil.isEnterKey(event);
            if (enter) dialog.dismiss();
            return enter;
        });
    }
}
