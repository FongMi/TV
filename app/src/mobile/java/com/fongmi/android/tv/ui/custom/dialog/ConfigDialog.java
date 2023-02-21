package com.fongmi.android.tv.ui.custom.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.DialogConfigBinding;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ConfigDialog {

    private final DialogConfigBinding binding;
    private final ConfigCallback callback;
    private final AlertDialog dialog;
    private String url;
    private int type;

    public static ConfigDialog create(Fragment fragment) {
        return new ConfigDialog(fragment);
    }

    public ConfigDialog type(int type) {
        this.type = type;
        return this;
    }

    public ConfigDialog(Fragment fragment) {
        this.callback = (ConfigCallback) fragment;
        this.binding = DialogConfigBinding.inflate(LayoutInflater.from(fragment.getContext()));
        this.dialog = new MaterialAlertDialogBuilder(fragment.getActivity()).setView(binding.getRoot()).create();
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
        binding.text.setText(url = getUrl());
        binding.text.setSelection(url.length());
    }

    private void initEvent() {
        binding.positive.setOnClickListener(this::onPositive);
        binding.negative.setOnClickListener(this::onNegative);
        binding.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) binding.positive.performClick();
            return true;
        });
    }

    private String getUrl() {
        switch (type) {
            case 0:
                return ApiConfig.getUrl();
            case 1:
                return LiveConfig.getUrl();
            case 2:
                return WallConfig.getUrl();
            default:
                return "";
        }
    }

    private void onPositive(View view) {
        String text = Utils.checkClan(binding.text.getText().toString().trim());
        if (text.isEmpty()) Config.delete(url, type);
        callback.setConfig(Config.find(text, type));
        dialog.dismiss();
    }

    private void onNegative(View view) {
        dialog.dismiss();
    }
}
