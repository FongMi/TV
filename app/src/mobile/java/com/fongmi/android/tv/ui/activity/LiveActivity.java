package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.LiveConfig;

public class LiveActivity extends BaseActivity {

    public static void start(Activity activity) {
        if (!LiveConfig.isEmpty()) activity.startActivity(new Intent(activity, LiveActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return null;
    }
}
