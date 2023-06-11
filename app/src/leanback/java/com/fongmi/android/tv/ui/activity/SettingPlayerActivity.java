package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivitySettingPlayerBinding;
import com.fongmi.android.tv.impl.UaCallback;
import com.fongmi.android.tv.player.ExoUtil;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.dialog.UaDialog;
import com.fongmi.android.tv.utils.Prefers;
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
        mBinding.uaText.setText(Prefers.getUa());
        mBinding.tunnelText.setText(getSwitch(Prefers.isTunnel()));
        mBinding.httpText.setText((http = ResUtil.getStringArray(R.array.select_player_http))[Prefers.getHttp()]);
        mBinding.tunnel.setVisibility(Players.isExo(Prefers.getPlayer()) ? View.VISIBLE : View.GONE);
        mBinding.http.setVisibility(Players.isExo(Prefers.getPlayer()) ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void initEvent() {
        mBinding.ua.setOnClickListener(this::onUa);
        mBinding.http.setOnClickListener(this::setHttp);
        mBinding.tunnel.setOnClickListener(this::setTunnel);
    }

    private void onUa(View view) {
        UaDialog.create(this).show();
    }

    private void setHttp(View view) {
        int index = Prefers.getHttp();
        Prefers.putHttp(index = index == http.length - 1 ? 0 : ++index);
        mBinding.httpText.setText(http[index]);
        ExoUtil.reset();
    }

    private void setTunnel(View view) {
        Prefers.putTunnel(!Prefers.isTunnel());
        mBinding.tunnelText.setText(getSwitch(Prefers.isTunnel()));
    }

    @Override
    public void setUa(String ua) {
        mBinding.uaText.setText(ua);
        Prefers.putUa(ua);
    }
}
