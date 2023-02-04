package com.fongmi.android.tv.ui.activity;

import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.api.Updater;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.databinding.ActivityMainBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.fragment.SettingFragment;
import com.fongmi.android.tv.ui.fragment.VodFragment;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements NavigationBarView.OnItemSelectedListener {

    private ActivityMainBinding mBinding;
    private List<Fragment> mFragments;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityMainBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        Updater.get().start();
        Server.get().start();
        initFragment();
        initConfig();
    }

    @Override
    protected void initEvent() {
        mBinding.navigation.setOnItemSelectedListener(this);
        mBinding.navigation.setSelectedItemId(R.id.vod);
    }

    private void initFragment() {
        mFragments = new ArrayList<>();
        mFragments.add(VodFragment.newInstance());
        mFragments.add(SettingFragment.newInstance());
        for (Fragment fragment : mFragments) getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).hide(fragment).commit();
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
                RefreshEvent.history();
                RefreshEvent.video();
            }

            @Override
            public void error(int resId) {
                Notify.show(resId);
            }
        };
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.vod:
                getSupportFragmentManager().beginTransaction().hide(mFragments.get(1)).show(mFragments.get(0)).commit();
                return true;
            case R.id.setting:
                getSupportFragmentManager().beginTransaction().hide(mFragments.get(0)).show(mFragments.get(1)).commit();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
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
