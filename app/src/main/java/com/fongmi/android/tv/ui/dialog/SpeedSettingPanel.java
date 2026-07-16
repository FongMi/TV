package com.fongmi.android.tv.ui.dialog;

import android.view.View;
import android.widget.TextView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogSpeedSettingBinding;
import com.fongmi.android.tv.databinding.ViewSettingSliderBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.setting.SpeedSetting;
import com.fongmi.android.tv.utils.SliderUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.slider.Slider;

final class SpeedSettingPanel {

    private final DialogSpeedSettingBinding binding;
    private final PlayerManager player;
    private final boolean save;
    private float speed;

    SpeedSettingPanel(DialogSpeedSettingBinding binding, PlayerManager player, boolean save) {
        this.binding = binding;
        this.player = player;
        this.save = save;
    }

    void bind() {
        bindSpeed();
        bindLongPressSpeed();
        bindPreset();
        bindSkipSilence();
        bindReset();
        syncControls();
        if (Util.isLeanback()) binding.speed.slider.requestFocus();
    }

    void release() {
    }

    private void bindSpeed() {
        ViewSettingSliderBinding item = binding.speed;
        Slider slider = item.slider;
        item.title.setText(R.string.speed_setting_current);
        slider.clearOnChangeListeners();
        SpeedSetting.setup(slider);
        slider.addOnChangeListener((source, value, fromUser) -> {
            if (!fromUser) return;
            setSpeed(value);
        });
    }

    private void bindLongPressSpeed() {
        ViewSettingSliderBinding item = binding.longPressSpeed;
        Slider slider = item.slider;
        item.title.setText(R.string.speed_setting_long_press);
        slider.clearOnChangeListeners();
        SpeedSetting.setupLongPress(slider);
        slider.addOnChangeListener((source, value, fromUser) -> {
            if (!fromUser) return;
            setLongPressSpeed(value);
        });
    }

    private void bindPreset() {
        float[] presets = SpeedSetting.getPresets();
        TextView[] views = getPresetViews();
        for (int i = 0; i < views.length; i++) {
            bindPreset(views[i], presets, i);
        }
    }

    private void bindSkipSilence() {
        binding.skipSilenceRow.setOnClickListener(view -> {
            if (binding.skipSilenceSwitch.isEnabled()) binding.skipSilenceSwitch.performClick();
        });
    }

    private void bindReset() {
        binding.reset.setOnClickListener(this::onReset);
        binding.reset.setNextFocusDownId(binding.speed.slider.getId());
    }

    private void onReset(View view) {
        resetAll();
    }

    private void resetAll() {
        setLongPressSpeedAndSync();
        setSkipSilenceEnabled(false);
        setSpeedAndSync(SpeedSetting.NORMAL);
    }

    private void bindPreset(TextView view, float[] presets, int index) {
        boolean visible = index < presets.length;
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) setSpeedOnClick(view, presets[index]);
    }

    private void setSpeedOnClick(TextView view, float speed) {
        view.setText(SpeedSetting.formatValue(speed));
        view.setOnClickListener(v -> setSpeedAndSync(speed));
    }

    private void setSpeedAndSync(float speed) {
        if (setSpeed(speed)) syncSpeedSlider();
    }

    private boolean setSpeed(float speed) {
        if (player == null || player.isReleased()) return false;
        speed = SpeedSetting.clamp(speed);
        this.speed = player.setSpeed(speed);
        updateSpeedValue();
        saveSpeed();
        return true;
    }

    private void syncControls() {
        speed = getSpeed();
        syncSpeedSlider();
        syncLongPressSpeedSlider();
        updateSkipSilenceSwitch();
    }

    private void syncSpeedSlider() {
        SliderUtil.setValue(binding.speed.slider, speed);
        updateSpeedValue();
    }

    private void syncLongPressSpeedSlider() {
        float speed = SpeedSetting.getLongPress();
        SliderUtil.setValue(binding.longPressSpeed.slider, speed);
        binding.longPressSpeed.value.setText(SpeedSetting.format(speed));
    }

    private void updateSpeedValue() {
        binding.speed.value.setText(SpeedSetting.format(speed));
    }

    private void setLongPressSpeedAndSync() {
        setLongPressSpeed(SpeedSetting.LONG_PRESS);
        syncLongPressSpeedSlider();
    }

    private void setLongPressSpeed(float speed) {
        speed = SpeedSetting.clampLongPress(speed);
        if (Float.compare(speed, SpeedSetting.getLongPress()) != 0) SpeedSetting.putLongPress(speed);
        binding.longPressSpeed.value.setText(SpeedSetting.format(speed));
    }

    private float getSpeed() {
        if (player == null || player.isReleased()) return SpeedSetting.NORMAL;
        return SpeedSetting.clamp(player.getSpeed());
    }

    private TextView[] getPresetViews() {
        return new TextView[]{binding.preset01, binding.preset02, binding.preset03, binding.preset04, binding.preset05, binding.preset06, binding.preset07, binding.preset08, binding.preset09, binding.preset10, binding.preset11, binding.preset12};
    }

    private void updateSkipSilenceSwitch() {
        boolean supported = canSkipSilence();
        binding.skipSilenceSwitch.setOnCheckedChangeListener(null);
        binding.skipSilenceSwitch.setChecked(supported && isSkipSilence());
        binding.skipSilenceSwitch.setEnabled(supported);
        binding.skipSilenceRow.setEnabled(supported);
        binding.skipSilenceRow.setAlpha(supported ? 1.0f : 0.38f);
        binding.skipSilenceSwitch.setOnCheckedChangeListener((button, checked) -> setSkipSilenceEnabled(checked));
    }

    private boolean canSkipSilence() {
        return player != null && !player.isReleased() && player.supportsSkipSilence();
    }

    private boolean isSkipSilence() {
        return player != null && !player.isReleased() && player.isSkipSilence();
    }

    private void setSkipSilenceEnabled(boolean enabled) {
        if (player == null || player.isReleased()) return;
        player.setSkipSilenceEnabled(enabled);
        updateSkipSilenceSwitch();
    }

    private void saveSpeed() {
        if (save) SpeedSetting.putPlayback(speed);
    }
}
