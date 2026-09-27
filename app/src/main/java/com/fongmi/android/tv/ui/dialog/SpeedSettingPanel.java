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
    private final float[] presets;
    private final TextView[] presetViews;
    private float speed;

    SpeedSettingPanel(DialogSpeedSettingBinding binding, PlayerManager player, boolean save) {
        this.binding = binding;
        this.player = player;
        this.save = save;
        this.presets = SpeedSetting.getPresets();
        this.presetViews = new TextView[]{binding.preset01, binding.preset02, binding.preset03, binding.preset04, binding.preset05, binding.preset06, binding.preset07, binding.preset08};
    }

    void bind() {
        bindSpeed();
        bindLongPressSpeed();
        for (int i = 0; i < presetViews.length; i++) bindPreset(presetViews[i], i);
        binding.reset.setOnClickListener(view -> resetAll());
        syncControls();
        updateLongPressSpeedNextFocus();
        PlaybackDialogFocus.request(binding.getRoot(), binding.speed.slider);
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

    private void resetAll() {
        setLongPressSpeed(SpeedSetting.LONG_PRESS);
        syncLongPressSpeedSlider();
        setSkipSilenceEnabled(false);
        setSpeedAndSync(SpeedSetting.NORMAL);
    }

    private void bindPreset(TextView view, int index) {
        boolean visible = index < presets.length;
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) return;
        float speed = presets[index];
        view.setText(SpeedSetting.formatValue(speed));
        view.setOnClickListener(v -> setSpeedAndSync(speed));
    }

    private void setSpeedAndSync(float speed) {
        if (setSpeed(speed)) syncSpeedSlider();
    }

    private boolean setSpeed(float speed) {
        if (player.isReleased()) return false;
        speed = SpeedSetting.clamp(speed);
        this.speed = player.setSpeed(speed);
        updateSpeedValue();
        updateLongPressSpeedNextFocus();
        if (save) SpeedSetting.putPlayback(this.speed);
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

    private void setLongPressSpeed(float speed) {
        speed = SpeedSetting.clampLongPress(speed);
        if (Float.compare(speed, SpeedSetting.getLongPress()) != 0) SpeedSetting.putLongPress(speed);
        binding.longPressSpeed.value.setText(SpeedSetting.format(speed));
    }

    private float getSpeed() {
        if (player.isReleased()) return SpeedSetting.NORMAL;
        return SpeedSetting.clamp(player.getSpeed());
    }

    private void updateLongPressSpeedNextFocus() {
        if (!Util.isLeanback()) return;
        int count = Math.min(presets.length, presetViews.length);
        if (count == 0) return;
        int nearest = 0;
        for (int i = 1; i < count; i++) {
            if (Math.abs(presets[i] - speed) < Math.abs(presets[nearest] - speed)) nearest = i;
        }
        binding.longPressSpeed.slider.setNextFocusDownId(presetViews[nearest].getId());
    }

    private void updateSkipSilenceSwitch() {
        boolean supported = !player.isReleased() && player.supportsSkipSilence();
        binding.skipSilenceSwitch.setOnCheckedChangeListener(null);
        binding.skipSilenceSwitch.setChecked(supported && player.isSkipSilence());
        binding.skipSilenceSwitch.setEnabled(supported);
        binding.skipSilenceRow.setEnabled(supported);
        binding.skipSilenceRow.setAlpha(supported ? 1.0f : 0.38f);
        binding.skipSilenceSwitch.setOnCheckedChangeListener((button, checked) -> setSkipSilenceEnabled(checked));
    }

    private void setSkipSilenceEnabled(boolean enabled) {
        if (player.isReleased()) return;
        player.setSkipSilenceEnabled(enabled);
        updateSkipSilenceSwitch();
    }

}
