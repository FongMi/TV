package com.fongmi.bear.ui;

import android.content.Intent;
import android.os.Handler;

import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.databinding.ActivitySplashBinding;

public class SplashActivity extends BaseActivity {

    @Override
    protected ViewBinding getBinding() {
        return ActivitySplashBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        loadConfig();
        openHome();
    }

    private void loadConfig() {

    }

    private void openHome() {
        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }, 2000);
    }
}
