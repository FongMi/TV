package com.fongmi.android.tv.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.ApiConfig;
import com.fongmi.android.tv.databinding.ActivitySplashBinding;
import com.fongmi.android.tv.net.Callback;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {

    private ActivitySplashBinding mBinding;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySplashBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.title.animate().alpha(1).setDuration(2000).setListener(onAnimationEnd()).start();
    }

    private AnimatorListenerAdapter onAnimationEnd() {
        return new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mBinding.title.setVisibility(View.GONE);
                mBinding.info.animate().alpha(1).setDuration(500).start();
                loadConfig();
            }
        };
    }

    private void loadConfig() {
        ApiConfig.get().init().loadConfig(new Callback() {
            @Override
            public void success() {

            }

            @Override
            public void error(int resId) {

            }
        });
    }
}
