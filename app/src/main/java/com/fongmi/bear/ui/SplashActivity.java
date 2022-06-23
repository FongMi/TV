package com.fongmi.bear.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.App;
import com.fongmi.bear.bean.Config;
import com.fongmi.bear.databinding.ActivitySplashBinding;
import com.fongmi.bear.net.Callback;
import com.fongmi.bear.net.OKHttp;
import com.fongmi.bear.utils.FileUtil;
import com.fongmi.bear.utils.Prefers;
import com.github.catvod.loader.JarLoader;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {

    private ActivitySplashBinding binding;

    @Override
    protected ViewBinding getBinding() {
        return binding = ActivitySplashBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        binding.title.animate().alpha(1).setDuration(2000).setListener(onAnimationEnd()).start();
    }

    private AnimatorListenerAdapter onAnimationEnd() {
        return new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                binding.title.setVisibility(View.GONE);
                binding.progress.animate().alpha(1).setDuration(500).start();
                binding.info.animate().alpha(1).setDuration(500).start();
                checkUrl();
            }
        };
    }

    private void checkUrl() {
        String url = Prefers.getString("url");
        if (url.isEmpty()) HomeActivity.start(getActivity());
        else getConfig(url);
    }

    private void getConfig(String url) {
        OKHttp.get().client().newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Config config = Config.objectFrom(response.body().string());
                App.get().setConfig(config);
                loadJar(config.getSpider());
            }
        });
    }

    private void loadJar(String url) {
        FileUtil.download(url, new Callback() {
            @Override
            public void onResponse(File file) {
                JarLoader.get().load();
                HomeActivity.start(getActivity());
            }
        });
    }
}
