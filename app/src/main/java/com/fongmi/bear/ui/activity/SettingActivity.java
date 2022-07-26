package com.fongmi.bear.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.ApiConfig;
import com.fongmi.bear.bean.Site;
import com.fongmi.bear.databinding.ActivitySettingBinding;
import com.fongmi.bear.databinding.DialogConfigBinding;
import com.fongmi.bear.databinding.DialogSiteBinding;
import com.fongmi.bear.net.Callback;
import com.fongmi.bear.ui.presenter.SitePresenter;
import com.fongmi.bear.utils.Notify;
import com.fongmi.bear.utils.Prefers;
import com.fongmi.bear.utils.ResUtil;

public class SettingActivity extends BaseActivity {

    private ActivitySettingBinding mBinding;

    public static void start(Activity activity) {
        activity.startActivityForResult(new Intent(activity, SettingActivity.class), 1000);
    }

    private final ActivityResultLauncher<String> launcherString = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> loadConfig());
    private final ActivityResultLauncher<Intent> launcherIntent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> loadConfig());

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.home.setText(ApiConfig.get().getHome().getName());
        mBinding.url.setText(Prefers.getUrl());
    }

    @Override
    protected void initEvent() {
        mBinding.config.setOnClickListener(this::showConfig);
        mBinding.site.setOnClickListener(this::showSite);
    }

    private void showConfig(View view) {
        DialogConfigBinding bindingDialog = DialogConfigBinding.inflate(LayoutInflater.from(this));
        bindingDialog.text.setText(Prefers.getUrl());
        bindingDialog.text.setSelection(bindingDialog.text.getText().length());
        AlertDialog dialog = Notify.show(this, bindingDialog.getRoot(), (dialogInterface, i) -> {
            Prefers.putUrl(bindingDialog.text.getText().toString().trim());
            mBinding.url.setText(Prefers.getUrl());
            Notify.progress(this);
            ApiConfig.get().clear();
            checkUrl();
        });
        bindingDialog.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            return true;
        });
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
        ApiConfig.get().loadConfig(new Callback() {
            @Override
            public void success() {
                mBinding.home.setText(ApiConfig.get().getHome().getName());
                setResult(RESULT_OK);
                Notify.dismiss();
            }

            @Override
            public void error(int resId) {
                mBinding.home.setText(ApiConfig.get().getHome().getName());
                setResult(RESULT_OK);
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
        for (int i = 0; i < adapter.size(); i++) ((Site) adapter.get(i)).setActivated(item);
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
        mBinding.home.setText(item.getName());
        ApiConfig.get().setHome(item);
        setResult(RESULT_OK);
        Notify.dismiss();
    }
}
