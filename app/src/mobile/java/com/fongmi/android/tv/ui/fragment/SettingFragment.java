package com.fongmi.android.tv.ui.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.FragmentSettingBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.ui.activity.BaseFragment;
import com.fongmi.android.tv.ui.custom.ConfigDialog;
import com.fongmi.android.tv.ui.custom.SiteDialog;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;

public class SettingFragment extends BaseFragment implements ConfigDialog.Callback, SiteDialog.Callback {

    private final ActivityResultLauncher<String> launcherString = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> loadConfig());
    private final ActivityResultLauncher<Intent> launcherIntent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> loadConfig());

    private FragmentSettingBinding mBinding;

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mBinding.url.setText(Prefers.getUrl());
        mBinding.home.setText(ApiConfig.getHomeName());
    }

    @Override
    protected void initEvent() {
        mBinding.site.setOnClickListener(v -> SiteDialog.show(this));
        mBinding.config.setOnClickListener(v -> ConfigDialog.show(this));
    }

    @Override
    public void setSite(Site item) {
        mBinding.home.setText(item.getName());
        ApiConfig.get().setHome(item);
        RefreshEvent.video();
    }

    @Override
    public void setConfig(String url) {
        mBinding.url.setText(url);
        Notify.progress(getActivity());
        AppDatabase.clear();
        Prefers.putUrl(url);
        checkUrl(url);
    }

    private void checkUrl(String url) {
        if (url.startsWith("file://") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            launcherIntent.launch(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        } else if (url.startsWith("file://") && Build.VERSION.SDK_INT < Build.VERSION_CODES.R && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            launcherString.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            loadConfig();
        }
    }

    private void loadConfig() {
        ApiConfig.get().clear().loadConfig(new Callback() {
            @Override
            public void success() {
                mBinding.home.setText(ApiConfig.getHomeName());
                RefreshEvent.history();
                RefreshEvent.video();
                Notify.dismiss();
            }

            @Override
            public void error(int resId) {
                mBinding.home.setText(ApiConfig.getHomeName());
                RefreshEvent.history();
                RefreshEvent.video();
                Notify.dismiss();
                Notify.show(resId);
            }
        });
    }
}
