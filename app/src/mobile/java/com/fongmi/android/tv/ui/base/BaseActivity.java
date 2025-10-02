package com.fongmi.android.tv.ui.base;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewGroup;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.ui.custom.CustomWallView;
import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public abstract class BaseActivity extends AppCompatActivity {

    private OnBackInvokedCallback callback;

    protected abstract ViewBinding getBinding();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableEdgeToEdge();
        setContentView(getBinding().getRoot());
        EventBus.getDefault().register(this);
        initView(savedInstanceState);
        setBackCallback();
        initEvent();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        if (!customWall()) return;
        ((ViewGroup) findViewById(android.R.id.content)).addView(new CustomWallView(this, null), 0, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    protected Activity getActivity() {
        return this;
    }

    protected boolean customWall() {
        return true;
    }

    protected void initView(Bundle savedInstanceState) {
    }

    protected void initEvent() {
    }

    protected boolean isVisible(View view) {
        return view.getVisibility() == View.VISIBLE;
    }

    protected boolean isGone(View view) {
        return view.getVisibility() == View.GONE;
    }

    protected void setPadding(ViewGroup layout) {
        setPadding(layout, false);
    }

    protected void setPadding(ViewGroup layout, boolean leftOnly) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        DisplayCutout cutout = ResUtil.getDisplay(this).getCutout();
        if (cutout == null) return;
        int top = cutout.getSafeInsetTop();
        int left = cutout.getSafeInsetLeft();
        int right = cutout.getSafeInsetRight();
        int bottom = cutout.getSafeInsetBottom();
        int padding = left | right | top | bottom;
        layout.setPadding(padding, 0, leftOnly ? 0 : padding, 0);
    }

    protected void noPadding(ViewGroup layout) {
        layout.setPadding(0, 0, 0, 0);
    }

    private void setBackCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback = this::onBackInvoked);
        } else {
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    onBackInvoked();
                }
            });
        }
    }

    private void enableEdgeToEdge() {
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSubscribe(Object o) {
    }

    protected void onBackInvoked() {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(callback);
    }
}
