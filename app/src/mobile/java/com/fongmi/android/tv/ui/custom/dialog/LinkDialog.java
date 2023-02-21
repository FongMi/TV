package com.fongmi.android.tv.ui.custom.dialog;

import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.databinding.DialogLinkBinding;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LinkDialog {

    private final DialogLinkBinding binding;
    private final AlertDialog dialog;

    public static LinkDialog create(Fragment fragment) {
        return new LinkDialog(fragment);
    }

    public LinkDialog(Fragment fragment) {
        this.binding = DialogLinkBinding.inflate(LayoutInflater.from(fragment.getContext()));
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
        CharSequence text = Utils.getClipText();
        if (!TextUtils.isEmpty(text) && Patterns.WEB_URL.matcher(text).matches()) binding.text.setText(text);
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
        String text = binding.text.getText().toString().trim();
        if (!text.isEmpty()) DetailActivity.push(App.activity(), text);
        dialog.dismiss();
    }

    private void onNegative(View view) {
        dialog.dismiss();
    }
}
