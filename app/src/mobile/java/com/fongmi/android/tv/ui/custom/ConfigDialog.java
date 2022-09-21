package com.fongmi.android.tv.ui.custom;

import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.databinding.DialogConfigBinding;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.utils.Prefers;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ConfigDialog {

    private DialogConfigBinding binding;
    private ConfigCallback callback;
    private AlertDialog dialog;

    public static void show(Fragment fragment) {
        new ConfigDialog().create(fragment);
    }

    public void create(Fragment fragment) {
        callback = (ConfigCallback) fragment;
        binding = DialogConfigBinding.inflate(LayoutInflater.from(fragment.getContext()));
        dialog = new MaterialAlertDialogBuilder(fragment.getContext()).setView(binding.getRoot()).create();
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.text.setText(Prefers.getUrl());
        binding.text.setSelection(binding.text.getText().length());
    }

    private void initEvent() {
        binding.positive.setOnClickListener(this::onPositive);
        binding.negative.setOnClickListener(this::onNegative);
        binding.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) binding.positive.performClick();
            return true;
        });
    }

    private void onPositive(View view) {
        String url = binding.text.getText().toString().trim();
        if (url.startsWith("clan")) url = url.replace("clan", "file");
        callback.setConfig(url);
        dialog.dismiss();
    }

    private void onNegative(View view) {
        dialog.dismiss();
    }
}
