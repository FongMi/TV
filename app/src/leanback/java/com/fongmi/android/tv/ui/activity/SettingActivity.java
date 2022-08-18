package com.fongmi.android.tv.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.SettingCallback;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.ActivitySettingBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.ui.custom.ConfigDialog;
import com.fongmi.android.tv.ui.custom.ConfigHistoryDialog;
import com.fongmi.android.tv.ui.custom.SiteDialog;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;

public class SettingActivity extends BaseActivity implements SettingCallback {

    private final ActivityResultLauncher<String> launcherString = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> loadConfig());
    private final ActivityResultLauncher<Intent> launcherIntent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> loadConfig());

    private ActivitySettingBinding mBinding;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.url.setText(Prefers.getUrl());
        mBinding.home.setText(ApiConfig.getHomeName());
        mBinding.compress.setText(ResUtil.getStringArray(R.array.select_thumbnail)[Prefers.getThumbnail()]);
    }

    @Override
    protected void initEvent() {
        mBinding.site.setOnClickListener(view -> SiteDialog.show(this));
        mBinding.config.setOnClickListener(view -> ConfigDialog.show(this));
        mBinding.history.setOnClickListener(view -> ConfigHistoryDialog.show(this));
        mBinding.thumbnail.setOnClickListener(this::setThumbnail);
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
        Notify.progress(this);
        Prefers.putUrl(url);
        checkUrl(url);
    }

    private void checkUrl(String url) {
        if (url.startsWith("file://") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            launcherIntent.launch(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        } else if (url.startsWith("file://") && Build.VERSION.SDK_INT < Build.VERSION_CODES.R && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            launcherString.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            loadConfig();
        }
    }

    private void loadConfig() {
        ApiConfig.get().clear().loadConfig(new Callback() {
            @Override
            public void success() {
                Config.save();
                setSite(0);
            }

            @Override
            public void error(int resId) {
                setSite(resId);
            }
        });
    }

    private void setSite(int resId) {
        mBinding.home.setText(ApiConfig.getHomeName());
        RefreshEvent.history();
        RefreshEvent.video();
        Notify.show(resId);
        Notify.dismiss();
    }

    public void setThumbnail(View view) {
        CharSequence[] array = ResUtil.getStringArray(R.array.select_thumbnail);
        int index = Prefers.getThumbnail();
        index = index == 2 ? 0 : ++index;
        Prefers.putThumbnail(index);
        mBinding.compress.setText(array[index]);
        RefreshEvent.image();
    }
}
