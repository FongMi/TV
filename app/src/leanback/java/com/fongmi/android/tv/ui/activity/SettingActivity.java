package com.fongmi.android.tv.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.ActivitySettingBinding;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.ui.custom.ConfigDialog;
import com.fongmi.android.tv.ui.presenter.SitePresenter;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.EventBus;

public class SettingActivity extends BaseActivity implements ConfigDialog.Callback {

    private ActivitySettingBinding mBinding;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingActivity.class));
    }

    private final ActivityResultLauncher<String> launcherString = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> loadConfig());
    private final ActivityResultLauncher<Intent> launcherIntent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> loadConfig());

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
        mBinding.site.setOnClickListener(this::showSite);
        mBinding.config.setOnClickListener(view -> ConfigDialog.show(this));
        mBinding.thumbnail.setOnClickListener(this::setThumbnail);
    }

    @Override
    public void setConfig() {
        mBinding.url.setText(Prefers.getUrl());
        Notify.progress(this);
        AppDatabase.clear();
        checkUrl();
    }

    private void checkUrl() {
        if (Prefers.getUrl().startsWith("file://") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            launcherIntent.launch(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        } else if (Prefers.getUrl().startsWith("file://") && Build.VERSION.SDK_INT < Build.VERSION_CODES.R && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
                EventBus.getDefault().post(RefreshEvent.history());
                EventBus.getDefault().post(RefreshEvent.video());
                Notify.dismiss();
            }

            @Override
            public void error(int resId) {
                mBinding.home.setText(ApiConfig.getHomeName());
                EventBus.getDefault().post(RefreshEvent.history());
                EventBus.getDefault().post(RefreshEvent.video());
                Notify.dismiss();
                Notify.show(resId);
            }
        });
    }

    private void showSite(View view) {
        if (ApiConfig.get().getSites().isEmpty()) return;
        int position = ApiConfig.get().getSites().indexOf(ApiConfig.get().getHome());
        DialogSiteBinding bindingDialog = DialogSiteBinding.inflate(LayoutInflater.from(this));
        SitePresenter presenter = new SitePresenter();
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
        adapter.addAll(0, ApiConfig.get().getSites());
        presenter.setOnClickListener(item -> setSite(adapter, item));
        bindingDialog.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        bindingDialog.recycler.setAdapter(new ItemBridgeAdapter(adapter));
        bindingDialog.recycler.scrollToPosition(position);
        Notify.show(this, bindingDialog.getRoot());
    }

    public void setSite(ArrayObjectAdapter adapter, Site item) {
        ApiConfig.get().setHome(item);
        mBinding.home.setText(item.getName());
        for (int i = 0; i < adapter.size(); i++) ((Site) adapter.get(i)).setHome(item);
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
        EventBus.getDefault().post(RefreshEvent.video());
        Notify.dismiss();
    }

    public void setThumbnail(View view) {
        CharSequence[] array = ResUtil.getStringArray(R.array.select_thumbnail);
        int index = Prefers.getThumbnail();
        index = index == 2 ? 0 : ++index;
        Prefers.putThumbnail(index);
        mBinding.compress.setText(array[index]);
        EventBus.getDefault().post(RefreshEvent.image());
    }
}
