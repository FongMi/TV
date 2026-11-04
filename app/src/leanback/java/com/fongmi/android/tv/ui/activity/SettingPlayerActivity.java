package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
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
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;

public class SettingPlayerActivity extends BaseActivity implements UaListener, BufferListener {

    private ActivitySettingPlayerBinding mBinding;
    private String[] render;
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
        setPlaybackModeText();
        mBinding.engine.requestFocus();
        mBinding.adblockText.setText(Setting.getSwitch(Setting.isAdblock()));
        mBinding.libassText.setText(Setting.getSwitch(PlayerSetting.isLibass()));
        mBinding.bufferText.setText(String.valueOf(PlayerSetting.getBuffer()));
        setMotionOptions();
        mBinding.backgroundText.setText(Setting.getSwitch(PlayerSetting.isBackgroundOn()));
    }

    @Override
    protected void initEvent() {
        mBinding.engine.setOnClickListener(this::setEngine);
        mBinding.decode.setOnClickListener(this::onDecodeSetting);
        mBinding.adblock.setOnClickListener(this::setAdblock);
        mBinding.libass.setOnClickListener(this::setLibass);
        mBinding.mpvConf.setOnClickListener(this::onMpvConf);
        mBinding.mpvGpuNext.setOnClickListener(this::setMpvGpuNext);
        mBinding.mpvVulkan.setOnClickListener(this::setMpvVulkan);
        mBinding.motion.setOnClickListener(this::setMotion);
        mBinding.render.setOnClickListener(this::setRender);
        mBinding.background.setOnClickListener(this::onBackground);
        mBinding.buffer.setOnClickListener(this::onBuffer);
        mBinding.preload.setOnClickListener(this::onPreloadSetting);
        mBinding.ua.setOnClickListener(this::onUa);
    }

    private void setVisible() {
        boolean exo = PlayerSetting.isExo();
        boolean vulkan = !exo && MpvUtil.isVulkanSupported();
        boolean motionSupported = exo ? Build.VERSION.SDK_INT >= Build.VERSION_CODES.O : vulkan;
        mBinding.motion.setVisibility(motionSupported ? View.VISIBLE : View.GONE);
        setMotionFocus(motionSupported);
        if (PlayerSetting.isBackgroundPiP()) PlayerSetting.putBackground(1);
        mBinding.exoRow.setVisibility(exo ? View.VISIBLE : View.GONE);
        mBinding.mpvRow.setVisibility(exo ? View.GONE : View.VISIBLE);
        mBinding.adblock.setNextFocusDownId(exo ? R.id.libass : R.id.mpvConf);
        mBinding.mpvConf.setNextFocusDownId(vulkan ? R.id.mpvVulkan : R.id.ua);
        mBinding.mpvGpuNext.setNextFocusDownId(vulkan ? R.id.mpvVulkan : R.id.ua);
        mBinding.ua.setNextFocusUpId(exo ? R.id.libass : vulkan ? R.id.mpvVulkan : R.id.mpvConf);
        mBinding.mpvVulkan.setVisibility(vulkan ? View.VISIBLE : View.GONE);
    }

    private void setMotionFocus(boolean available) {
        mBinding.decode.setNextFocusDownId(available ? R.id.motion : R.id.adblock);
        mBinding.preload.setNextFocusDownId(available ? R.id.motion : R.id.background);
        mBinding.adblock.setNextFocusUpId(available ? R.id.motion : R.id.decode);
        mBinding.background.setNextFocusUpId(available ? R.id.motion : R.id.preload);
    }

    private void setEngine(View view) {
        int index = (PlayerSetting.getEngine() + 1) % PlayerSetting.getEngineCount();
        if (index == PlayerSetting.ENGINE_MPV && !MpvUtil.checkInstalled()) return;
        PlayerSetting.putEngine(index);
        setPlaybackModeText();
        setVisible();
        setMpvOptions();
    }

    private void onMpvConf(View view) {
        MpvConfDialog.show(this);
    }

    private void setMpvGpuNext(View view) {
        PlayerSetting.putMpvGpuNext(!PlayerSetting.isMpvGpuNext());
        setMpvOptions();
    }

    private void setMpvVulkan(View view) {
        PlayerSetting.putMpvVulkan(!PlayerSetting.isMpvVulkan());
        setMpvOptions();
    }

    private void setMotion(View view) {
        String[] items = ResUtil.getStringArray(R.array.select_motion);
        int index = (PlayerSetting.getMotion() + 1) % items.length;
        PlayerSetting.putMotion(index);
        setMotionOptions();
        Notify.show(R.string.player_motion_restart);
    }

    private void setMotionOptions() {
        mBinding.motionText.setText(ResUtil.getStringArray(R.array.select_motion)[PlayerSetting.getMotion()]);
        setMpvOptions();
    }

    private void setMpvOptions() {
        boolean motion = MpvUtil.isMotionEnabled();
        mBinding.mpvGpuNextText.setText(Setting.getSwitch(PlayerSetting.isMpvGpuNext() || motion));
        mBinding.mpvVulkanText.setText(Setting.getSwitch(PlayerSetting.isMpvVulkan() || motion));
        mBinding.mpvGpuNext.setEnabled(!motion);
        mBinding.mpvVulkan.setEnabled(!motion);
        mBinding.mpvGpuNext.setAlpha(motion ? 0.5f : 1f);
        mBinding.mpvVulkan.setAlpha(motion ? 0.5f : 1f);
        mBinding.background.setNextFocusDownId(PlayerSetting.isExo() ? R.id.buffer : motion ? R.id.mpvConf : R.id.mpvGpuNext);
    }

    private void setRender(View view) {
        int index = (PlayerSetting.getRender() + 1) % render.length;
        PlayerSetting.putRender(index);
        setPlaybackModeText();
    }

    private void setPlaybackModeText() {
        engine = ResUtil.getStringArray(R.array.select_engine);
        render = ResUtil.getStringArray(R.array.select_render);
        mBinding.engineText.setText(engine[PlayerSetting.getEngine()]);
        mBinding.renderText.setText(render[PlayerSetting.getRender()]);
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

    private void setLibass(View view) {
        PlayerSetting.putLibass(!PlayerSetting.isLibass());
        mBinding.libassText.setText(Setting.getSwitch(PlayerSetting.isLibass()));
    }

    private void onPreloadSetting(View view) {
        SettingPreloadActivity.start(this);
    }

    private void onDecodeSetting(View view) {
        SettingDecodeActivity.start(this);
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
            setPlaybackModeText();
            setVisible();
            setMotionOptions();
        }
    }
}
