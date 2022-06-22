package com.fongmi.bear.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.R;
import com.fongmi.bear.databinding.ActivitySettingBinding;
import com.fongmi.bear.databinding.DialogConfigBinding;
import com.fongmi.bear.utils.Prefers;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingActivity extends BaseActivity {

    private ActivitySettingBinding binding;

    public static void newInstance(Activity activity) {
        activity.startActivity(new Intent(activity, SettingActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = ActivitySettingBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        binding.url.setText(Prefers.getString("url"));
    }

    @Override
    protected void initEvent() {
        binding.config.setOnClickListener(this::showConfig);
    }

    private void showConfig(View view) {
        DialogConfigBinding dialog = DialogConfigBinding.inflate(LayoutInflater.from(this));
        dialog.url.setText(Prefers.getString("url"));
        dialog.url.setSelection(dialog.url.getText().length());
        new MaterialAlertDialogBuilder(this, R.style.DialogTheme)
                .setView(dialog.getRoot())
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, (dialogInterface, i) -> {
                    Prefers.put("url", dialog.url.getText().toString().trim());
                    binding.url.setText(Prefers.getString("url"));
                }).show();

    }
}
