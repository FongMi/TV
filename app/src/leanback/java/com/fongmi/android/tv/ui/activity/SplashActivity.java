package com.fongmi.android.tv.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.databinding.ActivitySplashBinding;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.utils.Notify;

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
                HomeActivity.start(getActivity());
            }

            @Override
            public void error(int resId) {
                HomeActivity.start(getActivity());
                Notify.show(resId);
            }
        });
    }
}
