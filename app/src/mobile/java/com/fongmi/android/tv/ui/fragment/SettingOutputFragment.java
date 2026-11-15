package com.fongmi.android.tv.ui.fragment;

import android.content.DialogInterface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.FragmentSettingOutputBinding;
import com.fongmi.android.tv.player.mpv.MpvUtil;
import com.fongmi.android.tv.setting.OutputSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingOutputFragment extends BaseFragment {

    private FragmentSettingOutputBinding mBinding;
    private String[] renderModes;

    public static SettingOutputFragment newInstance() {
        return new SettingOutputFragment();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingOutputBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        renderModes = ResUtil.getStringArray(R.array.select_render);
        setVisible();
        refresh();
    }

    @Override
    protected void initEvent() {
        mBinding.render.setOnClickListener(this::setRender);
        mBinding.dolbyVisionOutput.setOnClickListener(this::showDolbyVisionOutput);
        mBinding.audioPassThrough.setOnClickListener(this::setAudioPassThrough);
        mBinding.tunnel.setOnClickListener(this::setTunnel);
        mBinding.motion.setOnClickListener(this::showMotion);
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
        updateDolbyVisionOutput();
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

    private void showDolbyVisionOutput(View view) {
        String[] items = ResUtil.getStringArray(R.array.select_dolby_vision_output);
        new MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.player_dolby_vision_output).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(items, OutputSetting.getDolbyVisionOutputPolicy(), this::onDolbyVisionOutput).show();
    }

    private void onDolbyVisionOutput(DialogInterface dialog, int which) {
        OutputSetting.putDolbyVisionOutputPolicy(which);
        updateDolbyVisionOutput();
        dialog.dismiss();
    }

    private void updateDolbyVisionOutput() {
        mBinding.dolbyVisionOutputText.setText(ResUtil.getStringArray(R.array.select_dolby_vision_output)[OutputSetting.getDolbyVisionOutputPolicy()]);
    }

    private void setAudioPassThrough(View view) {
        OutputSetting.putAudioPassThrough(!OutputSetting.isAudioPassThrough());
        mBinding.audioPassThroughText.setText(Setting.getSwitch(OutputSetting.isAudioPassThrough()));
    }

    private void showMotion(View view) {
        String[] items = ResUtil.getStringArray(R.array.select_motion);
        new MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.player_motion).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(items, OutputSetting.getMotion(), this::onMotion).show();
    }

    private void onMotion(DialogInterface dialog, int which) {
        OutputSetting.putMotion(which);
        updateMotion();
        setVisible();
        Notify.show(R.string.player_motion_restart);
        dialog.dismiss();
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

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) initView();
    }
}
