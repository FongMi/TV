package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivityMainBinding;
import com.fongmi.android.tv.ui.fragment.HomeFragment;
import com.fongmi.android.tv.ui.fragment.SettingFragment;
import com.fongmi.android.tv.ui.fragment.VodFragment;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements NavigationBarView.OnItemSelectedListener {

    private ActivityMainBinding mBinding;
    private List<Fragment> mFragments;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, MainActivity.class));
        activity.finish();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityMainBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mFragments = new ArrayList<>();
        mFragments.add(HomeFragment.newInstance());
        mFragments.add(VodFragment.newInstance());
        mFragments.add(SettingFragment.newInstance());
        for (Fragment fragment : mFragments) getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).hide(fragment).commit();
    }

    @Override
    protected void initEvent() {
        mBinding.navigation.setOnItemSelectedListener(this);
        mBinding.navigation.setSelectedItemId(R.id.home);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:
                getSupportFragmentManager().beginTransaction().show(mFragments.get(0)).hide(mFragments.get(1)).hide(mFragments.get(2)).commit();
                return true;
            case R.id.vod:
                getSupportFragmentManager().beginTransaction().show(mFragments.get(1)).hide(mFragments.get(0)).hide(mFragments.get(2)).commit();
                return true;
            case R.id.setting:
                getSupportFragmentManager().beginTransaction().show(mFragments.get(2)).hide(mFragments.get(0)).hide(mFragments.get(1)).commit();
                return true;
            default:
                return false;
        }
    }
}
