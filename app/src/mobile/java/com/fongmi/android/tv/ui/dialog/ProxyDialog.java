package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogProxyBinding;
import com.fongmi.android.tv.impl.ProxyCallback;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ProxyDialog {

    private final DialogProxyBinding binding;
    private final ProxyCallback callback;
    private AlertDialog dialog;
    private boolean append;

    public static ProxyDialog create(Fragment fragment) {
        return new ProxyDialog(fragment);
    }

    public ProxyDialog(Fragment fragment) {
        this.callback = (ProxyCallback) fragment;
        this.binding = DialogProxyBinding.inflate(LayoutInflater.from(fragment.getContext()));
        this.append = true;
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.setting_proxy).setView(binding.getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        String text = Setting.getProxy();
        binding.text.setText(text);
        binding.text.setSelection(TextUtils.isEmpty(text) ? 0 : text.length());
    }

    private void initEvent() {
        binding.text.addTextChangedListener(new CustomTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                detect(s.toString());
            }
        });
        binding.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            return true;
        });
    }

    private void detect(String s) {
        if (append && "h".equalsIgnoreCase(s)) {
            append = false;
            binding.text.append("ttp://");
        } else if (append && "s".equalsIgnoreCase(s)) {
            append = false;
            binding.text.append("ocks5://");
        } else if (append && s.length() == 1) {
            append = false;
            binding.text.getText().insert(0, "socks5://");
        } else if (s.length() > 1) {
            append = false;
        } else if (s.length() == 0) {
            append = true;
        }
    }

    private void onPositive(DialogInterface dialog, int which) {
        callback.setProxy(binding.text.getText().toString().trim());
        dialog.dismiss();
    }

    private void onNegative(DialogInterface dialog, int which) {
        dialog.dismiss();
    }
}
