package com.fongmi.bear.ui;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.databinding.ActivitySettingBinding;
import com.fongmi.bear.utils.Prefers;

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
        binding.config.setOnClickListener(view -> Toast.makeText(SettingActivity.this, "config", Toast.LENGTH_SHORT).show());
    }
}
