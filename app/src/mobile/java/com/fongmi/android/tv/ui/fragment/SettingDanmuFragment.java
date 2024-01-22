package com.fongmi.android.tv.ui.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.FragmentSettingDanmuBinding;
import com.fongmi.android.tv.impl.DanmuAlphaCallback;
import com.fongmi.android.tv.impl.DanmuLineCallback;
import com.fongmi.android.tv.impl.DanmuSizeCallback;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.dialog.DanmuAlphaDialog;
import com.fongmi.android.tv.ui.dialog.DanmuLineDialog;
import com.fongmi.android.tv.ui.dialog.DanmuSizeDialog;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingDanmuFragment extends BaseFragment implements DanmuLineCallback, DanmuSizeCallback, DanmuAlphaCallback {

    private FragmentSettingDanmuBinding mBinding;
    private String[] danmuSpeed;

    public static SettingDanmuFragment newInstance() {
        return new SettingDanmuFragment();
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingDanmuBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mBinding.danmuLoadText.setText(getSwitch(Setting.isDanmuLoad()));
        mBinding.danmuSizeText.setText(String.valueOf(Setting.getDanmuSize()));
        mBinding.danmuLineText.setText(String.valueOf(Setting.getDanmuLine(2)));
        mBinding.danmuAlphaText.setText(String.valueOf(Setting.getDanmuAlpha()));
        mBinding.danmuSpeedText.setText((danmuSpeed = ResUtil.getStringArray(R.array.select_danmu_speed))[Setting.getDanmuSpeed()]);
    }

    @Override
    protected void initEvent() {
        mBinding.danmuSize.setOnClickListener(this::onDanmuSize);
        mBinding.danmuLine.setOnClickListener(this::onDanmuLine);
        mBinding.danmuLoad.setOnClickListener(this::setDanmuLoad);
        mBinding.danmuAlpha.setOnClickListener(this::onDanmuAlpha);
        mBinding.danmuSpeed.setOnClickListener(this::onDanmuSpeed);
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

    @Override
    public void setDanmuLine(int line) {
        mBinding.danmuLineText.setText(String.valueOf(line));
        Setting.putDanmuLine(line);
    }

    private void setDanmuLoad(View view) {
        Setting.putDanmuLoad(!Setting.isDanmuLoad());
        mBinding.danmuLoadText.setText(getSwitch(Setting.isDanmuLoad()));
    }

    private void onDanmuAlpha(View view) {
        DanmuAlphaDialog.create(this).show();
    }

    @Override
    public void setDanmuAlpha(int alpha) {
        mBinding.danmuAlphaText.setText(String.valueOf(alpha));
        Setting.putDanmuAlpha(alpha);
    }

    private void onDanmuSpeed(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.player_danmu_speed).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(danmuSpeed, Setting.getDanmuSpeed(), (dialog, which) -> {
            mBinding.danmuSpeedText.setText(danmuSpeed[which]);
            Setting.putDanmuSpeed(which);
            dialog.dismiss();
        }).show();
    }

}
