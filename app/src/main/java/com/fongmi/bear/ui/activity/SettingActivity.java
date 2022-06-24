package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.ApiConfig;
import com.fongmi.bear.databinding.ActivitySettingBinding;
import com.fongmi.bear.databinding.DialogConfigBinding;
import com.fongmi.bear.net.Callback;
import com.fongmi.bear.utils.Notify;
import com.fongmi.bear.utils.Prefers;

public class SettingActivity extends BaseActivity {

    private ActivitySettingBinding binding;

    public static void start(Activity activity) {
        activity.startActivityForResult(new Intent(activity, SettingActivity.class), 1000);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = ActivitySettingBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        binding.url.setText(Prefers.getUrl());
    }

    @Override
    protected void initEvent() {
        binding.config.setOnClickListener(this::showConfig);
    }

    private void showConfig(View view) {
        DialogConfigBinding bindingDialog = DialogConfigBinding.inflate(LayoutInflater.from(this));
        bindingDialog.url.setText(Prefers.getUrl());
        bindingDialog.url.setSelection(bindingDialog.url.getText().length());
        Notify.show(this, bindingDialog.getRoot(), (dialogInterface, i) -> {
            Prefers.put("url", bindingDialog.url.getText().toString().trim());
            binding.url.setText(Prefers.getUrl());
            reloadConfig();
        });
    }

    //TODO SHOW PROGRESS
    private void reloadConfig() {
        ApiConfig.get().loadConfig(new Callback() {
            @Override
            public void success() {
                setResult(RESULT_OK);
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
            }
        });
    }
}
