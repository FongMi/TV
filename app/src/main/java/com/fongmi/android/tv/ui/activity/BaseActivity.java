package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

public abstract class BaseActivity extends AppCompatActivity {

    protected abstract ViewBinding getBinding();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getBinding().getRoot());
        EventBus.getDefault().register(this);
        Utils.hideSystemUI(this);
        setWall();
        initView();
        initEvent();
    }

    protected Activity getActivity() {
        return this;
    }

    protected void initView() {
    }

    protected void initEvent() {
    }

    private void setWall() {
        try {
            File file = FileUtil.getWall(Prefers.getWall());
            if (file.exists() && file.length() > 0) getWindow().setBackgroundDrawable(WallConfig.drawable(Drawable.createFromPath(file.getPath())));
            else getWindow().setBackgroundDrawableResource(ResUtil.getDrawable(file.getName()));
        } catch (Exception e) {
            getWindow().setBackgroundDrawableResource(R.drawable.wallpaper_1);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() != RefreshEvent.Type.WALL) return;
        WallConfig.get().setDrawable(null);
        setWall();
    }

    @Override
    public Resources getResources() {
        return Product.hackResources(super.getResources());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Utils.hideSystemUI(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) Utils.hideSystemUI(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
