package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivitySettingDanmakuBinding;
import com.fongmi.android.tv.impl.DanmakuListener;
import com.fongmi.android.tv.setting.DanmakuSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.DanmakuApiDialog;
import com.fongmi.android.tv.ui.dialog.DanmakuSettingDialog;

public class SettingDanmakuActivity extends BaseActivity implements DanmakuListener {

    private ActivitySettingDanmakuBinding mBinding;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingDanmakuActivity.class));
    }

    private String getApiStatus() {
        return getString(TextUtils.isEmpty(DanmakuSetting.getEffectiveApiUrl()) ? R.string.none : R.string.yes);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingDanmakuBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mBinding.danmakuLoad.requestFocus();
        mBinding.danmakuApiText.setText(getApiStatus());
        mBinding.danmakuAutoText.setText(Setting.getSwitch(DanmakuSetting.isAuto()));
        mBinding.danmakuLoadText.setText(Setting.getSwitch(DanmakuSetting.isLoad()));
        mBinding.danmakuSpiderText.setText(Setting.getSwitch(DanmakuSetting.isSpiderFirst()));
        updateApiVisibility();
    }

    @Override
    protected void initEvent() {
        mBinding.danmakuApi.setOnClickListener(this::onDanmakuApi);
        mBinding.danmakuAuto.setOnClickListener(this::setDanmakuAuto);
        mBinding.danmakuLoad.setOnClickListener(this::setDanmakuLoad);
        mBinding.danmakuSpider.setOnClickListener(this::setDanmakuSpider);
        mBinding.danmakuDensity.setOnClickListener(view -> DanmakuSettingDialog.create().tab(2).show(this));
    }

    private void setDanmakuLoad(View view) {
        DanmakuSetting.putLoad(!DanmakuSetting.isLoad());
        mBinding.danmakuLoadText.setText(Setting.getSwitch(DanmakuSetting.isLoad()));
        updateApiVisibility();
    }

    private void updateApiVisibility() {
        boolean load = DanmakuSetting.isLoad();
        mBinding.danmakuApi.setVisibility(load ? View.VISIBLE : View.GONE);
        updateAutoVisibility();
    }

    private void updateAutoVisibility() {
        boolean show = DanmakuSetting.isLoad() && !TextUtils.isEmpty(DanmakuSetting.getEffectiveApiUrl());
        mBinding.danmakuAuto.setVisibility(show ? View.VISIBLE : View.GONE);
        updateSpiderVisibility();
    }

    private void updateSpiderVisibility() {
        boolean show = DanmakuSetting.isLoad() && !TextUtils.isEmpty(DanmakuSetting.getEffectiveApiUrl()) && DanmakuSetting.isAuto();
        mBinding.danmakuSpider.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void onDanmakuApi(View view) {
        DanmakuApiDialog.show(this);
    }

    @Override
    public void setDanmakuApi(String url) {
        DanmakuSetting.putApiUrl(url);
        mBinding.danmakuApiText.setText(getApiStatus());
        updateAutoVisibility();
    }

    private void setDanmakuAuto(View view) {
        DanmakuSetting.putAuto(!DanmakuSetting.isAuto());
        mBinding.danmakuAutoText.setText(Setting.getSwitch(DanmakuSetting.isAuto()));
        updateSpiderVisibility();
    }

    private void setDanmakuSpider(View view) {
        DanmakuSetting.putSpiderFirst(!DanmakuSetting.isSpiderFirst());
        mBinding.danmakuSpiderText.setText(Setting.getSwitch(DanmakuSetting.isSpiderFirst()));
    }
}
