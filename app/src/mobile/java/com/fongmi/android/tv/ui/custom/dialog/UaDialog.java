package com.fongmi.android.tv.ui.custom.dialog;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogUaBinding;
import com.fongmi.android.tv.impl.UaCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class UaDialog {

    private final DialogUaBinding binding;
    private final UaCallback callback;
    private AlertDialog dialog;

    public static UaDialog create(Fragment fragment) {
        return new UaDialog(fragment);
    }

    public UaDialog(Fragment fragment) {
        this.callback = (UaCallback) fragment;
        this.binding = DialogUaBinding.inflate(LayoutInflater.from(fragment.getContext()));
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.setting_player_ua).setView(binding.getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        String ua = Setting.getUa();
        binding.text.setText(ua);
        binding.text.setSelection(TextUtils.isEmpty(ua) ? 0 : ua.length());
    }

    private void initEvent() {
        binding.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            return true;
        });
    }

    private void onPositive(DialogInterface dialog, int which) {
        callback.setUa(binding.text.getText().toString().trim());
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        dialog.dismiss();
    }
}
