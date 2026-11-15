package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.media3.common.DolbyVisionOutputPolicy;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivitySettingOutputBinding;
import com.fongmi.android.tv.player.mpv.MpvUtil;
import com.fongmi.android.tv.setting.OutputSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;

public class SettingOutputActivity extends BaseActivity {

    private ActivitySettingOutputBinding mBinding;
    private String[] renderModes;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingOutputActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingOutputBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        renderModes = ResUtil.getStringArray(R.array.select_render);
        setVisible();
        mBinding.render.requestFocus();
        refresh();
    }

    @Override
    protected void initEvent() {
        mBinding.render.setOnClickListener(this::setRender);
        mBinding.dolbyVisionOutput.setOnClickListener(this::setDolbyVisionOutput);
        mBinding.audioPassThrough.setOnClickListener(this::setAudioPassThrough);
        mBinding.tunnel.setOnClickListener(this::setTunnel);
        mBinding.motion.setOnClickListener(this::setMotion);
        mBinding.mpvGpuNext.setOnClickListener(this::setMpvGpuNext);
        mBinding.mpvVulkan.setOnClickListener(this::setMpvVulkan);
        mBinding.aac.setOnClickListener(this::setAAC);
    }

    private void setVisible() {
        boolean exo = PlayerSetting.isExo();
        boolean vulkan = !exo && MpvUtil.isVulkanSupported();
        boolean motionSupported = exo ? Build.VERSION.SDK_INT >= Build.VERSION_CODES.O : vulkan;
        boolean showMpvOptions = !exo && !MpvUtil.isMotionEnabled();
        mBinding.tunnel.setVisibility(exo ? View.VISIBLE : View.GONE);
        mBinding.motion.setVisibility(motionSupported ? View.VISIBLE : View.GONE);
        mBinding.mpvGpuNext.setVisibility(showMpvOptions ? View.VISIBLE : View.GONE);
        mBinding.mpvVulkan.setVisibility(showMpvOptions && vulkan ? View.VISIBLE : View.GONE);
        mBinding.aac.setVisibility(exo ? View.VISIBLE : View.GONE);
    }

    private void refresh() {
        updateRenderAndTunnel();
        updateMotion();
        updateDolbyVisionOutput(OutputSetting.getDolbyVisionOutputPolicy());
        mBinding.audioPassThroughText.setText(Setting.getSwitch(OutputSetting.isAudioPassThrough()));
        mBinding.mpvGpuNextText.setText(Setting.getSwitch(OutputSetting.isMpvGpuNext()));
        mBinding.mpvVulkanText.setText(Setting.getSwitch(OutputSetting.isMpvVulkan()));
        mBinding.aacText.setText(Setting.getSwitch(OutputSetting.isPreferAAC()));
    }

    private void setRender(View view) {
        OutputSetting.putRender((OutputSetting.getRender() + 1) % renderModes.length);
        updateRenderAndTunnel();
    }

    private void setTunnel(View view) {
        if (PlayerSetting.isMpv()) return;
        OutputSetting.putTunnel(!OutputSetting.isTunnel());
        updateRenderAndTunnel();
    }

    private void updateRenderAndTunnel() {
        mBinding.renderText.setText(renderModes[OutputSetting.getRender()]);
        mBinding.tunnelText.setText(Setting.getSwitch(OutputSetting.isTunnel()));
    }

    private void setDolbyVisionOutput(View view) {
        int mode = (OutputSetting.getDolbyVisionOutputPolicy() + 1) % (DolbyVisionOutputPolicy.ASSUME_UNSUPPORTED + 1);
        OutputSetting.putDolbyVisionOutputPolicy(mode);
        updateDolbyVisionOutput(mode);
    }

    private void updateDolbyVisionOutput(@DolbyVisionOutputPolicy.Mode int mode) {
        mBinding.dolbyVisionOutputText.setText(ResUtil.getStringArray(R.array.select_dolby_vision_output)[mode]);
    }

    private void setAudioPassThrough(View view) {
        OutputSetting.putAudioPassThrough(!OutputSetting.isAudioPassThrough());
        mBinding.audioPassThroughText.setText(Setting.getSwitch(OutputSetting.isAudioPassThrough()));
    }

    private void setMotion(View view) {
        String[] items = ResUtil.getStringArray(R.array.select_motion);
        int index = (OutputSetting.getMotion() + 1) % items.length;
        OutputSetting.putMotion(index);
        updateMotion();
        setVisible();
        Notify.show(R.string.player_motion_restart);
    }

    private void updateMotion() {
        mBinding.motionText.setText(ResUtil.getStringArray(R.array.select_motion)[OutputSetting.getMotion()]);
    }

    private void setMpvGpuNext(View view) {
        OutputSetting.putMpvGpuNext(!OutputSetting.isMpvGpuNext());
        mBinding.mpvGpuNextText.setText(Setting.getSwitch(OutputSetting.isMpvGpuNext()));
    }

    private void setMpvVulkan(View view) {
        OutputSetting.putMpvVulkan(!OutputSetting.isMpvVulkan());
        mBinding.mpvVulkanText.setText(Setting.getSwitch(OutputSetting.isMpvVulkan()));
    }

    private void setAAC(View view) {
        OutputSetting.putPreferAAC(!OutputSetting.isPreferAAC());
        mBinding.aacText.setText(Setting.getSwitch(OutputSetting.isPreferAAC()));
    }
}
