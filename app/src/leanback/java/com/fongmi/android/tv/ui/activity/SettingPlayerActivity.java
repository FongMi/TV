package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.ActivitySettingPlayerBinding;
import com.fongmi.android.tv.impl.UaCallback;
import com.fongmi.android.tv.player.ExoUtil;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.dialog.UaDialog;
import com.fongmi.android.tv.utils.ResUtil;

public class SettingPlayerActivity extends BaseActivity implements UaCallback {

    private ActivitySettingPlayerBinding mBinding;
    private String[] http;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingPlayerActivity.class));
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingPlayerBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.uaText.setText(Setting.getUa());
        mBinding.tunnelText.setText(getSwitch(Setting.isTunnel()));
        mBinding.manualText.setText(getSwitch(Setting.isManual()));
        mBinding.http.setVisibility(Players.isExo(Setting.getPlayer()) ? View.VISIBLE : View.GONE);
        mBinding.tunnel.setVisibility(Players.isExo(Setting.getPlayer()) ? View.VISIBLE : View.GONE);
        mBinding.httpText.setText((http = ResUtil.getStringArray(R.array.select_exo_http))[Setting.getHttp()]);
    }

    @Override
    protected void initEvent() {
        mBinding.ua.setOnClickListener(this::onUa);
        mBinding.http.setOnClickListener(this::setHttp);
        mBinding.tunnel.setOnClickListener(this::setTunnel);
        mBinding.manual.setOnClickListener(this::setManual);
    }

    private void onUa(View view) {
        UaDialog.create(this).show();
    }

    private void setHttp(View view) {
        int index = Setting.getHttp();
        Setting.putHttp(index = index == http.length - 1 ? 0 : ++index);
        mBinding.httpText.setText(http[index]);
        ExoUtil.reset();
    }

    private void setTunnel(View view) {
        Setting.putTunnel(!Setting.isTunnel());
        mBinding.tunnelText.setText(getSwitch(Setting.isTunnel()));
    }

    private void setManual(View view) {
        Setting.putManual(!Setting.isManual());
        mBinding.manualText.setText(getSwitch(Setting.isManual()));
    }

    @Override
    public void setUa(String ua) {
        mBinding.uaText.setText(ua);
        Setting.putUa(ua);
    }
}
