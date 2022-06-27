package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.ApiConfig;
import com.fongmi.bear.bean.Site;
import com.fongmi.bear.databinding.ActivitySettingBinding;
import com.fongmi.bear.databinding.DialogConfigBinding;
import com.fongmi.bear.databinding.DialogSiteBinding;
import com.fongmi.bear.net.Callback;
import com.fongmi.bear.ui.adapter.SiteAdapter;
import com.fongmi.bear.utils.Notify;
import com.fongmi.bear.utils.Prefers;

public class SettingActivity extends BaseActivity {

    private ActivitySettingBinding mBinding;
    private SiteAdapter mAdapter;

    public static void start(Activity activity) {
        activity.startActivityForResult(new Intent(activity, SettingActivity.class), 1000);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.home.setText(ApiConfig.get().getHome().getName());
        mBinding.url.setText(Prefers.getUrl());
        mAdapter = new SiteAdapter();
    }

    @Override
    protected void initEvent() {
        mBinding.config.setOnClickListener(this::showConfig);
        mBinding.site.setOnClickListener(this::showSite);
        mAdapter.setOnItemClickListener(this::onSiteClick);
    }

    private void showConfig(View view) {
        DialogConfigBinding bindingDialog = DialogConfigBinding.inflate(LayoutInflater.from(this));
        bindingDialog.url.setText(Prefers.getUrl());
        bindingDialog.url.setSelection(bindingDialog.url.getText().length());
        Notify.show(this, bindingDialog.getRoot(), (dialogInterface, i) -> {
            Prefers.putUrl(bindingDialog.url.getText().toString().trim());
            mBinding.url.setText(Prefers.getUrl());
            Notify.progress(this);
            loadConfig();
        });
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
            public void error(String msg) {
                Notify.show(msg);
            }
        });
    }

    private void showSite(View view) {
        if (ApiConfig.get().getSites().isEmpty()) return;
        int position = ApiConfig.get().getSites().indexOf(ApiConfig.get().getHome());
        DialogSiteBinding bindingDialog = DialogSiteBinding.inflate(LayoutInflater.from(this));
        bindingDialog.site.setLayoutManager(new LinearLayoutManager(this));
        bindingDialog.site.getItemAnimator().setChangeDuration(0);
        bindingDialog.site.setHasFixedSize(true);
        bindingDialog.site.setAdapter(mAdapter);
        mAdapter.addAll(ApiConfig.get().getSites());
        bindingDialog.site.scrollToPosition(position);
        Notify.show(this, bindingDialog.getRoot());
    }

    public void onSiteClick(Site item) {
        mBinding.home.setText(item.getName());
        ApiConfig.get().setHome(item);
        setResult(RESULT_OK);
    }
}
