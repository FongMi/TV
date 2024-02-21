package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.ActivitySettingDanmuBinding;
import com.fongmi.android.tv.impl.DanmuAlphaCallback;
import com.fongmi.android.tv.impl.DanmuLineCallback;
import com.fongmi.android.tv.impl.DanmuSizeCallback;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.DanmuAlphaDialog;
import com.fongmi.android.tv.ui.dialog.DanmuLineDialog;
import com.fongmi.android.tv.ui.dialog.DanmuSizeDialog;
import com.fongmi.android.tv.utils.ResUtil;

public class SettingDanmuActivity extends BaseActivity implements DanmuLineCallback, DanmuSizeCallback, DanmuAlphaCallback {

    private ActivitySettingDanmuBinding mBinding;

    private String[] danmuSpeed;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingDanmuBinding.inflate(getLayoutInflater());
    }

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingDanmuActivity.class));
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected void initView() {
        mBinding.danmuLoad.requestFocus();
        mBinding.danmuLoadText.setText(getSwitch(Setting.isDanmuLoad()));
        mBinding.danmuSizeText.setText(String.valueOf(Setting.getDanmuSize()));
        mBinding.danmuLineText.setText(String.valueOf(Setting.getDanmuLine(3)));
        mBinding.danmuAlphaText.setText(String.valueOf(Setting.getDanmuAlpha()));
        mBinding.danmuSpeedText.setText((danmuSpeed = ResUtil.getStringArray(R.array.select_danmu_speed))[Setting.getDanmuSpeed()]);
    }

    @Override
    protected void initEvent() {
        mBinding.danmuSize.setOnClickListener(this::onDanmuSize);
        mBinding.danmuLine.setOnClickListener(this::onDanmuLine);
        mBinding.danmuLoad.setOnClickListener(this::setDanmuLoad);
        mBinding.danmuAlpha.setOnClickListener(this::onDanmuAlpha);
        mBinding.danmuSpeed.setOnClickListener(this::setDanmuSpeed);
    }

    private void onDanmuSize(View view) {
        DanmuSizeDialog.create(this).show();
    }

    @Override
    public void setDanmuSize(float size) {
        mBinding.danmuSizeText.setText(String.valueOf(size));
        Setting.putDanmuSize(size);
    }

    private void onDanmuLine(View view) {
        DanmuLineDialog.create(this).show();
    }

    private void setDanmuLoad(View view) {
        Setting.putDanmuLoad(!Setting.isDanmuLoad());
        mBinding.danmuLoadText.setText(getSwitch(Setting.isDanmuLoad()));
    }

    @Override
    public void setDanmuLine(int line) {
        mBinding.danmuLineText.setText(String.valueOf(line));
        Setting.putDanmuLine(line);
    }

    private void onDanmuAlpha(View view) {
        DanmuAlphaDialog.create(this).show();
    }

    @Override
    public void setDanmuAlpha(int alpha) {
        mBinding.danmuAlphaText.setText(String.valueOf(alpha));
        Setting.putDanmuAlpha(alpha);
    }

    private void setDanmuSpeed(View view) {
        int index = Setting.getDanmuSpeed();
        Setting.putDanmuSpeed(index = index == danmuSpeed.length - 1 ? 0 : ++index);
        mBinding.danmuSpeedText.setText(danmuSpeed[index]);
    }


}
