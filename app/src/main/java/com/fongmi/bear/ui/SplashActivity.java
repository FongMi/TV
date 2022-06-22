package com.fongmi.bear.ui;

import android.annotation.SuppressLint;
import android.os.Handler;

import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.App;
import com.fongmi.bear.bean.Config;
import com.fongmi.bear.databinding.ActivitySplashBinding;
import com.fongmi.bear.net.Callback;
import com.fongmi.bear.net.Task;
import com.fongmi.bear.utils.Prefers;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {

    @Override
    protected ViewBinding getBinding() {
        return ActivitySplashBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        String url = Prefers.getString("url");
        if (url.isEmpty()) openHome();
        else Task.create(getCallback()).run(url);
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void onResponse(String result) {
                App.get().setConfig(Config.objectFrom(result));
                openHome();
            }
        };
    }

    private void openHome() {
        new Handler().postDelayed(() -> HomeActivity.newInstance(this), 500);
    }
}
