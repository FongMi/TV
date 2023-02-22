package com.fongmi.android.tv.ui.activity;

import android.content.Intent;
import android.util.Patterns;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
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
import com.fongmi.android.tv.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements NavigationBarView.OnItemSelectedListener {

    private ActivityMainBinding mBinding;
    private List<Fragment> mFragments;
    private boolean confirm;

    private VodFragment getVodFragment() {
        return (VodFragment) getSupportFragmentManager().findFragmentByTag("0");
    }

    private SettingFragment getSettingFragment() {
        return (SettingFragment) getSupportFragmentManager().findFragmentByTag("1");
    }

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
    protected void initView() {
        Notify.progress(this);
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
        for (int i = 0; i < mFragments.size(); i++) getSupportFragmentManager().beginTransaction().add(R.id.container, mFragments.get(i), String.valueOf(i)).hide(mFragments.get(i)).commitNowAllowingStateLoss();
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
                RefreshEvent.video();
                Notify.dismiss();
            }

            @Override
            public void error(int resId) {
                Notify.show(resId);
                Notify.dismiss();
            }
        };
    }

    private void checkAction(Intent intent) {
        if (ApiConfig.get().getSite("push_agent") == null) return;
        boolean hasClip = Patterns.WEB_URL.matcher(Utils.getClip()).matches();
        boolean hasAction = intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SEND) && intent.getType().equals("text/plain");
        if (hasAction) DetailActivity.start(this, "push_agent", intent.getStringExtra(Intent.EXTRA_TEXT), "");
        else if (hasClip) showDialog(Utils.getClip());
    }

    private void showDialog(String text) {
        new MaterialAlertDialogBuilder(this).setMessage(getString(R.string.home_open_url, text))
                .setNegativeButton(R.string.dialog_negative, (dialog, which) -> Utils.clearClip())
                .setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
                    DetailActivity.start(MainActivity.this, "push_agent", text, "");
                    Utils.clearClip();
                }).show();
    }

    private void setConfirm() {
        confirm = true;
        Notify.show(R.string.app_exit);
        App.post(() -> confirm = false, 2000);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.vod:
                getSupportFragmentManager().beginTransaction().hide(mFragments.get(1)).show(mFragments.get(0)).commitNowAllowingStateLoss();
                return true;
            case R.id.setting:
                getSupportFragmentManager().beginTransaction().hide(mFragments.get(0)).show(mFragments.get(1)).commitNowAllowingStateLoss();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (getSettingFragment().isVisible()) {
            mBinding.navigation.setSelectedItemId(R.id.vod);
        } else if (getVodFragment().canBack()) {
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
