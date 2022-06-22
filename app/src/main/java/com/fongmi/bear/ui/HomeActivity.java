package com.fongmi.bear.ui;

import android.app.Activity;
import android.content.Intent;

import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.App;
import com.fongmi.bear.databinding.ActivityHomeBinding;

public class HomeActivity extends BaseActivity {

    private ActivityHomeBinding binding;

    public static void newInstance(Activity activity) {
        activity.startActivity(new Intent(activity, HomeActivity.class));
        activity.finish();
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        if (App.get().getConfig() == null) {
            SettingActivity.newInstance(this);
        } else {

        }
    }
}