package com.fongmi.android.tv.ui.custom.dialog;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogLinkBinding;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LinkDialog {

    private final DialogLinkBinding binding;
    private final Fragment fragment;
    private AlertDialog dialog;

    public static LinkDialog create(Fragment fragment) {
        return new LinkDialog(fragment);
    }

    public LinkDialog(Fragment fragment) {
        this.fragment = fragment;
        this.binding = DialogLinkBinding.inflate(LayoutInflater.from(fragment.getContext()));
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.play).setView(binding.getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        CharSequence text = Utils.getClipText();
        binding.input.setEndIconOnClickListener(this::onChoose);
        if (!TextUtils.isEmpty(text) && Patterns.WEB_URL.matcher(text).matches()) binding.text.setText(text);
    }

    private void initEvent() {
        binding.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            return true;
        });
    }

    private void onChoose(View view) {
        FileChooser.from(fragment).show();
        dialog.dismiss();
    }

    private void onPositive(DialogInterface dialog, int which) {
        String text = binding.text.getText().toString().trim();
        if (!text.isEmpty()) DetailActivity.push(App.activity(), text);
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        dialog.dismiss();
    }
}
