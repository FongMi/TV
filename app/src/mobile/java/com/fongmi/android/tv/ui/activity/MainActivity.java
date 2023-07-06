package com.fongmi.android.tv.ui.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.databinding.ActivityMainBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.FragmentStateManager;
import com.fongmi.android.tv.ui.fragment.SettingFragment;
import com.fongmi.android.tv.ui.fragment.SettingPlayerFragment;
import com.fongmi.android.tv.ui.fragment.VodFragment;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends BaseActivity implements NavigationBarView.OnItemSelectedListener {

    private FragmentStateManager mManager;
    private ActivityMainBinding mBinding;
    private boolean confirm;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityMainBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAction(intent);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        initFragment(savedInstanceState);
        Updater.get().release().start();
        Server.get().start();
        initConfig();
    }

    @Override
    protected void initEvent() {
        mBinding.navigation.setOnItemSelectedListener(this);
    }

    private void checkAction(Intent intent) {
        boolean push = ApiConfig.hasPush() && intent.getAction() != null;
        if (push && intent.getAction().equals(Intent.ACTION_SEND)) {
            DetailActivity.push(this, Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT)));
        } else if (push && intent.getAction().equals(Intent.ACTION_VIEW) && intent.getData() != null) {
            DetailActivity.push(this, intent.getData());
        }
    }

    private void initFragment(Bundle savedInstanceState) {
        mManager = new FragmentStateManager(mBinding.container, getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                if (position == 0) return VodFragment.newInstance();
                if (position == 1) return SettingFragment.newInstance();
                if (position == 2) return SettingPlayerFragment.newInstance();
                return null;
            }
        };
        if (savedInstanceState == null) mManager.change(0);
    }

    private void initConfig() {
        WallConfig.get().init();
        LiveConfig.get().init();
        ApiConfig.get().init().load(getCallback());
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                checkAction(getIntent());
                RefreshEvent.config();
                RefreshEvent.video();
            }

            @Override
            public void error(String msg) {
                RefreshEvent.config();
                RefreshEvent.empty();
                Notify.show(msg);
            }
        };
    }

    private void setNavigation() {
        mBinding.navigation.getMenu().findItem(R.id.vod).setVisible(true);
        mBinding.navigation.getMenu().findItem(R.id.setting).setVisible(true);
        mBinding.navigation.getMenu().findItem(R.id.live).setVisible(LiveConfig.hasUrl());
    }

    private boolean openLive() {
        LiveActivity.start(this);
        return false;
    }

    private void setConfirm() {
        confirm = true;
        Notify.show(R.string.app_exit);
        App.post(() -> confirm = false, 2000);
    }

    public void change(int position) {
        mManager.change(position);
    }

    @Override
    public void onRefreshEvent(RefreshEvent event) {
        super.onRefreshEvent(event);
        if (event.getType().equals(RefreshEvent.Type.CONFIG)) setNavigation();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (mBinding.navigation.getSelectedItemId() == item.getItemId()) return false;
        if (item.getItemId() == R.id.vod) return mManager.change(0);
        if (item.getItemId() == R.id.setting) return mManager.change(1);
        if (item.getItemId() == R.id.live) return openLive();
        return false;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        RefreshEvent.video();
    }

    @Override
    public void onBackPressed() {
        if (!mBinding.navigation.getMenu().findItem(R.id.vod).isVisible()) {
            setNavigation();
        } else if (mManager.isVisible(2)) {
            change(1);
        } else if (mManager.isVisible(1)) {
            mBinding.navigation.setSelectedItemId(R.id.vod);
        } else if (mManager.canBack(0)) {
            if (!confirm) setConfirm();
            else finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WallConfig.get().clear();
        LiveConfig.get().clear();
        ApiConfig.get().clear();
        Server.get().stop();
    }
}
