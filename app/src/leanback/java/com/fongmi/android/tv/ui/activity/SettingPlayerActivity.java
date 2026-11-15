package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivitySettingPlayerBinding;
import com.fongmi.android.tv.impl.BufferListener;
import com.fongmi.android.tv.impl.UaListener;
import com.fongmi.android.tv.player.mpv.MpvUtil;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.BufferDialog;
import com.fongmi.android.tv.ui.dialog.MpvConfDialog;
import com.fongmi.android.tv.ui.dialog.UaDialog;
import com.fongmi.android.tv.utils.ResUtil;

public class SettingPlayerActivity extends BaseActivity implements UaListener, BufferListener {

    private ActivitySettingPlayerBinding mBinding;
    private String[] engine;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingPlayerActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingPlayerBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        setVisible();
        updateEngine();
        mBinding.engine.requestFocus();
        mBinding.adblockText.setText(Setting.getSwitch(Setting.isAdblock()));
        mBinding.bufferText.setText(String.valueOf(PlayerSetting.getBuffer()));
        mBinding.backgroundText.setText(Setting.getSwitch(PlayerSetting.isBackgroundOn()));
    }

    @Override
    protected void initEvent() {
        mBinding.engine.setOnClickListener(this::setEngine);
        mBinding.output.setOnClickListener(this::onOutputSetting);
        mBinding.adblock.setOnClickListener(this::setAdblock);
        mBinding.mpvConf.setOnClickListener(this::onMpvConf);
        mBinding.background.setOnClickListener(this::onBackground);
        mBinding.buffer.setOnClickListener(this::onBuffer);
        mBinding.preload.setOnClickListener(this::onPreloadSetting);
        mBinding.ua.setOnClickListener(this::onUa);
    }

    private void setVisible() {
        boolean exo = PlayerSetting.isExo();
        if (PlayerSetting.isBackgroundPiP()) PlayerSetting.putBackground(1);
        mBinding.buffer.setVisibility(exo ? View.VISIBLE : View.GONE);
        mBinding.mpvConf.setVisibility(exo ? View.GONE : View.VISIBLE);
        int nextFocusDownId = exo ? R.id.buffer : R.id.ua;
        mBinding.adblock.setNextFocusDownId(nextFocusDownId);
        mBinding.background.setNextFocusDownId(nextFocusDownId);
        mBinding.ua.setNextFocusUpId(exo ? R.id.buffer : View.NO_ID);
    }

    private void setEngine(View view) {
        int index = (PlayerSetting.getEngine() + 1) % PlayerSetting.getEngineCount();
        if (index == PlayerSetting.ENGINE_MPV && !MpvUtil.checkInstalled()) return;
        PlayerSetting.putEngine(index);
        updateEngine();
        setVisible();
    }

    private void onMpvConf(View view) {
        MpvConfDialog.show(this);
    }

    private void updateEngine() {
        engine = ResUtil.getStringArray(R.array.select_engine);
        mBinding.engineText.setText(engine[PlayerSetting.getEngine()]);
    }

    private void onBuffer(View view) {
        BufferDialog.show(this);
    }

    @Override
    public void setBuffer(int times) {
        mBinding.bufferText.setText(String.valueOf(times));
        PlayerSetting.putBuffer(times);
    }

    private void onBackground(View view) {
        PlayerSetting.putBackground(PlayerSetting.isBackgroundOn() ? 0 : 1);
        mBinding.backgroundText.setText(Setting.getSwitch(PlayerSetting.isBackgroundOn()));
    }

    private void setAdblock(View view) {
        Setting.putAdblock(!Setting.isAdblock());
        mBinding.adblockText.setText(Setting.getSwitch(Setting.isAdblock()));
    }

    private void onPreloadSetting(View view) {
        SettingPreloadActivity.start(this);
    }

    private void onOutputSetting(View view) {
        SettingOutputActivity.start(this);
    }

    private void onUa(View view) {
        UaDialog.show(this);
    }

    @Override
    public void setUa(String ua) {
        Setting.putUa(ua);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBinding != null) {
            updateEngine();
            setVisible();
        }
    }
}
