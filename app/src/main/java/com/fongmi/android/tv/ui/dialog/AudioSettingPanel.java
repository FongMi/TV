package com.fongmi.android.tv.ui.dialog;

import android.annotation.SuppressLint;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.media3.common.Format;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogAudioSettingBinding;
import com.fongmi.android.tv.databinding.ViewSettingSliderBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.effect.audio.AudioChannelMode;
import com.fongmi.android.tv.player.effect.audio.AudioEffectBands;
import com.fongmi.android.tv.player.effect.audio.AudioEffectConfig;
import com.fongmi.android.tv.player.effect.audio.AudioEffectPreset;
import com.fongmi.android.tv.player.effect.audio.AudioPresetLevels;
import com.fongmi.android.tv.setting.AudioSetting;
import com.fongmi.android.tv.utils.SliderUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AudioSettingPanel {

    private static final int LEVEL_STEP = 20;
    private static final int STABILITY_STEP = 20;
    private static final int DIALOGUE_STEP = 5;
    private static final int BOOST_STEP = 100;
    private static final int PREAMP_STEP = 100;
    private static final int CENTER_GAIN_STEP = 100;
    private static final int BALANCE_STEP = 5;
    private static final int MIN_AUDIO_OFFSET_MS = -10000;
    private static final int MAX_AUDIO_OFFSET_MS = 10000;
    private static final int AUDIO_OFFSET_STEP_MS = 100;

    private final DialogAudioSettingBinding binding;
    private final PlayerManager player;
    private final List<ViewSettingSliderBinding> bandViews;
    private AudioEffectBands bands;
    private int currentTab;
    private boolean previewOriginal;

    AudioSettingPanel(DialogAudioSettingBinding binding, PlayerManager player) {
        this.binding = binding;
        this.player = player;
        this.bandViews = new ArrayList<>();
        this.bands = AudioEffectBands.EMPTY;
    }

    void bind() {
        bands = getAudioBands();
        updatePresetCheck();
        bindSliders();
        bindChannelMode();
        bindSwitches();
        bindTabs();
        bindCompare();
        bindReset();
        showTab(0);
        updateControls();
        if (Util.isLeanback()) binding.tabPreset.requestFocus();
        binding.tabGroup.check(binding.tabPreset.getId());
    }

    void release() {
        binding.tabGroup.clearOnButtonCheckedListeners();
        previewOriginal(false);
    }

    private void onPresetChecked(ChipGroup group, List<Integer> checkedIds) {
        if (checkedIds.isEmpty()) updatePresetCheck();
        else applyPreset(group, checkedIds.get(0));
    }

    private void applyPreset(ChipGroup group, int chipId) {
        clearOtherPresetGroups(group);
        previewOriginal(false);
        setAudioSetting(presetForChip(chipId));
        bindBands();
        updateControls();
    }

    private void updatePresetCheck() {
        int chip = chipForPreset(getAppliedPreset());
        setPresetListeners(null);
        clearPresetGroups();
        checkPresetGroup(chip);
        setPresetListeners(this::onPresetChecked);
    }

    private ChipGroup[] getPresetGroups() {
        return new ChipGroup[]{binding.presetBasicGroup, binding.presetToneGroup, binding.presetGenreGroup, binding.presetCustomGroup};
    }

    private void setPresetListeners(ChipGroup.OnCheckedStateChangeListener listener) {
        for (ChipGroup group : getPresetGroups()) group.setOnCheckedStateChangeListener(listener);
    }

    private void clearPresetGroups() {
        for (ChipGroup group : getPresetGroups()) group.clearCheck();
    }

    private void clearOtherPresetGroups(ChipGroup checkedGroup) {
        setPresetListeners(null);
        for (ChipGroup group : getPresetGroups()) if (group != checkedGroup) group.clearCheck();
        setPresetListeners(this::onPresetChecked);
    }

    private void checkPresetGroup(int chip) {
        if (chip == View.NO_ID) return;
        for (ChipGroup group : getPresetGroups()) {
            if (group.findViewById(chip) != null) {
                group.check(chip);
                break;
            }
        }
    }

    private void bindSliders() {
        bindOptions();
        setupOffsetSlider();
        bindBands();
    }

    private void bindOptions() {
        setupOptionSlider(binding.stability, R.string.audio_effect_stability, AudioSetting.MIN_STABILITY, AudioSetting.MAX_STABILITY, STABILITY_STEP, AudioSetting.getStability(), this::formatStability, AudioSetting::putStability);
        setupOptionSlider(binding.boost, R.string.audio_effect_boost, AudioSetting.MIN_BOOST, AudioSetting.MAX_BOOST, BOOST_STEP, AudioSetting.getBoost(), this::formatLevel, AudioSetting::putBoost);
        setupOptionSlider(binding.dialogue, R.string.audio_effect_dialogue, AudioSetting.MIN_DIALOGUE, AudioSetting.MAX_DIALOGUE, DIALOGUE_STEP, AudioSetting.getDialogue(), this::formatPercent, AudioSetting::putDialogue);
        setupOptionSlider(binding.preamp, R.string.audio_effect_headroom, -AudioSetting.MAX_PREAMP, -AudioSetting.MIN_PREAMP, PREAMP_STEP, -AudioSetting.getPreamp(), this::formatPreamp, value -> AudioSetting.putPreamp(-value));
        setupOptionSlider(binding.centerGain, R.string.audio_effect_center_gain, AudioSetting.MIN_CENTER_GAIN, AudioSetting.MAX_CENTER_GAIN, CENTER_GAIN_STEP, AudioSetting.getCenterGain(), this::formatLevel, AudioSetting::putCenterGain);
        setupOptionSlider(binding.balance, R.string.audio_effect_balance, AudioSetting.MIN_BALANCE, AudioSetting.MAX_BALANCE, BALANCE_STEP, AudioSetting.getBalance(), this::formatBalance, AudioSetting::putBalance);
    }

    private void bindBands() {
        bandViews.clear();
        binding.bands.removeAllViews();
        if (bands.isEmpty()) return;
        short[] levels = AudioSetting.isEnabled() ? AudioSetting.getLevels(bands) : AudioPresetLevels.of(AudioEffectPreset.OFF, bands);
        LayoutInflater inflater = LayoutInflater.from(binding.bands.getContext());
        for (int i = 0; i < bands.getCount(); i++) {
            ViewSettingSliderBinding item = ViewSettingSliderBinding.inflate(inflater, binding.bands, false);
            setupBandSlider(item, i, levels[i]);
            bandViews.add(item);
            binding.bands.addView(item.getRoot());
        }
    }

    private void onChannelModeChecked(ChipGroup group, List<Integer> checkedIds) {
        if (checkedIds.isEmpty()) bindChannelMode();
        else setChannelMode(channelModeForChip(checkedIds.get(0)));
    }

    private void setChannelMode(int mode) {
        if (!AudioChannelMode.isAvailable(mode, getAudioChannelCount())) return;
        if (AudioSetting.getChannelMode() == mode) return;
        previewOriginal(false);
        AudioSetting.putChannelMode(mode);
        apply();
    }

    private void bindChannelMode() {
        int channelCount = getAudioChannelCount();
        int mode = AudioChannelMode.resolve(AudioSetting.getChannelMode(), channelCount);
        binding.channelModeGroup.setOnCheckedStateChangeListener(null);
        binding.channelModeGroup.check(chipForChannelMode(mode));
        binding.channelModeGroup.setOnCheckedStateChangeListener(this::onChannelModeChecked);
    }

    private void bindSwitches() {
        setupSwitch(binding.loudness, AudioSetting.isLoudnessEnabled(), AudioSetting::putLoudness);
    }

    private void setupSwitch(MaterialSwitch item, boolean checked, BooleanSetter setter) {
        item.setOnCheckedChangeListener(null);
        item.setChecked(checked);
        item.setOnCheckedChangeListener((button, enabled) -> {
            previewOriginal(false);
            setter.set(enabled);
            apply();
        });
    }

    private void bindTabs() {
        MaterialButton[] tabs = getTabs();
        for (MaterialButton tab : tabs) checkOnFocus(tab);
        binding.tabGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            for (int i = 0; i < tabs.length; i++) if (checkedId == tabs[i].getId()) showTab(i);
        });
    }

    private void checkOnFocus(MaterialButton button) {
        if (!Util.isLeanback()) return;
        button.setOnFocusChangeListener((view, focused) -> {
            if (focused) binding.tabGroup.check(button.getId());
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindCompare() {
        binding.compare.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) previewOriginal(true);
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) previewOriginal(false);
            return false;
        });
        binding.compare.setOnKeyListener((view, keyCode, event) -> {
            boolean supported = keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER;
            if (supported && event.getAction() == KeyEvent.ACTION_DOWN) previewOriginal(true);
            else if (supported && event.getAction() == KeyEvent.ACTION_UP) previewOriginal(false);
            return false;
        });
    }

    private void bindReset() {
        binding.reset.setOnClickListener(this::onReset);
        binding.reset.setOnLongClickListener(view -> {
            resetAll();
            return true;
        });
    }

    private void onReset(View view) {
        previewOriginal(false);
        switch (currentTab) {
            case 0 -> resetPreset();
            case 1 -> resetEqualizer();
            case 2 -> resetOffset();
            case 3 -> resetAdvanced();
        }
    }

    private void resetPreset() {
        AudioSetting.putPreset(AudioEffectPreset.OFF);
        updatePresetCheck();
        bindBands();
        apply();
    }

    private void resetEqualizer() {
        if (bands.isEmpty()) return;
        AudioSetting.putCustomLevels(bands, AudioPresetLevels.of(AudioEffectPreset.OFF, bands));
        if (AudioSetting.isEnabled()) AudioSetting.putPreset(AudioEffectPreset.CUSTOM);
        updatePresetCheck();
        bindBands();
        apply();
    }

    private void resetAdvanced() {
        AudioSetting.resetAdvanced();
        bindChannelMode();
        bindOptions();
        bindSwitches();
        apply();
    }

    private void resetOffset() {
        setAudioOffsetMs(0);
        setupOffsetSlider();
    }

    private void resetAll() {
        previewOriginal(false);
        AudioSetting.reset();
        setAudioOffsetMs(0);
        syncControls();
        apply();
    }

    private void setupOptionSlider(ViewSettingSliderBinding item, int titleRes, int min, int max, int stepSize, int initial, ValueFormatter formatter, ValueSetter setter) {
        Slider slider = item.slider;
        item.title.setText(titleRes);
        slider.clearOnChangeListeners();
        slider.setValueFrom(min);
        slider.setValueTo(max);
        slider.setStepSize(stepSize);
        slider.setLabelFormatter(formatter::format);
        SliderUtil.setValue(slider, initial);
        item.value.setText(formatter.format(slider.getValue()));
        slider.addOnChangeListener((source, value, fromUser) -> {
            if (!fromUser) return;
            previewOriginal(false);
            float snapped = SliderUtil.snap(source, value);
            item.value.setText(formatter.format(snapped));
            setter.set(Math.round(snapped));
            apply();
        });
    }

    private void setupOffsetSlider() {
        Slider slider = binding.offset.slider;
        binding.offset.title.setText(R.string.audio_offset);
        slider.clearOnChangeListeners();
        slider.setValueFrom(MIN_AUDIO_OFFSET_MS);
        slider.setValueTo(MAX_AUDIO_OFFSET_MS);
        slider.setStepSize(AUDIO_OFFSET_STEP_MS);
        slider.setLabelFormatter(this::formatOffset);
        SliderUtil.setValue(slider, getAudioOffsetMs());
        binding.offset.value.setText(formatOffset(slider.getValue()));
        slider.addOnChangeListener((source, value, fromUser) -> {
            if (!fromUser) return;
            previewOriginal(false);
            float snapped = SliderUtil.snap(source, value);
            binding.offset.value.setText(formatOffset(snapped));
            setAudioOffsetMs(Math.round(snapped));
        });
    }

    private void setupBandSlider(ViewSettingSliderBinding item, int index, short initial) {
        Slider slider = item.slider;
        int stepSize = getStepSize();
        item.title.setText(formatFrequency(bands.getCenterFrequency(index)));
        slider.clearOnChangeListeners();
        slider.setValueFrom(bands.getMinLevel());
        slider.setValueTo(bands.getMaxLevel());
        slider.setStepSize(stepSize);
        slider.setLabelFormatter(this::formatLevel);
        SliderUtil.setValue(slider, bands.snapToStep(initial, stepSize));
        item.value.setText(formatLevel(slider.getValue()));
        slider.addOnChangeListener((source, value, fromUser) -> {
            if (!fromUser) return;
            previewOriginal(false);
            float snapped = SliderUtil.snap(source, value);
            item.value.setText(formatLevel(snapped));
            short[] levels = getCurrentLevels();
            levels[index] = bands.snapToStep(Math.round(snapped), stepSize);
            AudioSetting.putCustomLevels(bands, levels);
            switchToCustom();
            apply();
        });
    }

    private void switchToCustom() {
        if (AudioSetting.isEnabled() && AudioSetting.getPreset() == AudioEffectPreset.CUSTOM) return;
        AudioSetting.putPreset(AudioEffectPreset.CUSTOM);
        updatePresetCheck();
    }

    private short[] getCurrentLevels() {
        short[] levels = new short[bandViews.size()];
        int stepSize = getStepSize();
        for (int i = 0; i < bandViews.size(); i++) levels[i] = bands.snapToStep(Math.round(bandViews.get(i).slider.getValue()), stepSize);
        return levels;
    }

    private void updateControls() {
        boolean supported = cancelUnsupportedPreview(canSetAudioSetting());
        boolean hasBands = !bands.isEmpty();
        int channelCount = getAudioChannelCount();
        boolean active = AudioSetting.hasEffect(channelCount);
        boolean channelMix = AudioChannelMode.isAvailable(channelCount);
        boolean centerGain = AudioEffectConfig.isCenterGainAvailable(channelCount);
        boolean balance = channelMix && AudioChannelMode.canBalance(AudioSetting.getChannelMode());
        updateUnsupported(supported);
        updateVisibility(hasBands, channelCount, channelMix, centerGain, balance);
        updateAvailability(supported, hasBands, active, balance);
    }

    private boolean cancelUnsupportedPreview(boolean supported) {
        if (supported || !previewOriginal) return supported;
        previewOriginal = false;
        binding.compare.setSelected(false);
        if (isPlayerAvailable()) player.previewAudioSetting(false);
        return canSetAudioSetting();
    }

    private void updateVisibility(boolean hasBands, int channelCount, boolean channelMix, boolean centerGain, boolean balance) {
        binding.dialogue.getRoot().setVisibility(hasBands ? View.VISIBLE : View.GONE);
        binding.channelSection.setVisibility(channelMix ? View.VISIBLE : View.GONE);
        binding.channelStereo.setVisibility(channelCount > 2 ? View.VISIBLE : View.GONE);
        binding.centerGain.getRoot().setVisibility(centerGain ? View.VISIBLE : View.GONE);
        binding.balance.getRoot().setVisibility(balance ? View.VISIBLE : View.GONE);
    }

    private void updateAvailability(boolean supported, boolean hasBands, boolean active, boolean balance) {
        boolean enabled = supported && AudioSetting.isEnabled();
        applyEnabled(binding.compare, supported && active);
        applyEnabled(binding.presetSection, supported && hasBands);
        applyEnabled(binding.audioSettingOptions, supported);
        applyEnabled(binding.dialogue.getRoot(), supported && hasBands);
        applyEnabled(binding.balance.getRoot(), supported && balance);
        applyEnabled(binding.bands, enabled && hasBands);
    }

    private void updateUnsupported(boolean supported) {
        binding.unsupported.setVisibility(supported ? View.GONE : View.VISIBLE);
        if (!supported) binding.unsupported.setText(getUnsupportedText());
    }

    private void applyEnabled(View view, boolean enabled) {
        view.setAlpha(enabled ? 1.0f : 0.38f);
        setEnabledRecursive(view, enabled);
    }

    private void setEnabledRecursive(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) setEnabledRecursive(group.getChildAt(i), enabled);
        }
    }

    private void apply() {
        if (isPlayerAvailable()) player.refreshAudioSetting();
        updateControls();
    }

    private void syncControls() {
        bands = getAudioBands();
        updatePresetCheck();
        bindChannelMode();
        bindSliders();
        bindSwitches();
    }

    private void setAudioSetting(int preset) {
        if (!isPlayerAvailable()) AudioSetting.putPreset(preset);
        else player.setAudioSetting(preset);
    }

    private boolean canSetAudioSetting() {
        return isPlayerAvailable() && player.canSetAudioSetting();
    }

    private AudioEffectBands getAudioBands() {
        return isPlayerAvailable() ? player.getAudioSettingBands() : AudioEffectBands.EMPTY;
    }

    private int getAudioChannelCount() {
        return isPlayerAvailable() ? player.getAudioChannelCount() : Format.NO_VALUE;
    }

    private int getAudioOffsetMs() {
        long value = isPlayerAvailable() ? player.getAudioOffsetMs() : 0;
        return Math.clamp(value, MIN_AUDIO_OFFSET_MS, MAX_AUDIO_OFFSET_MS);
    }

    private void setAudioOffsetMs(int offsetMs) {
        if (isPlayerAvailable()) player.setAudioOffsetMs(offsetMs);
    }

    private int getUnsupportedText() {
        int reason = isPlayerAvailable() ? player.getAudioSettingError() : 0;
        return reason == 0 ? R.string.error_audio_effect_unsupported : reason;
    }

    private boolean isPlayerAvailable() {
        return player != null && !player.isReleased();
    }

    private void showTab(int index) {
        View[] roots = {binding.presetSection, binding.equalizerSection, binding.offsetSection, binding.advancedSection};
        MaterialButton[] tabs = getTabs();
        for (int i = 0; i < roots.length; i++) roots[i].setVisibility(index == i ? View.VISIBLE : View.GONE);
        binding.reset.setNextFocusDownId(tabs[currentTab = index].getId());
    }

    private MaterialButton[] getTabs() {
        return new MaterialButton[]{binding.tabPreset, binding.tabEqualizer, binding.tabOffset, binding.tabAdvanced};
    }

    private void previewOriginal(boolean original) {
        if (previewOriginal == original) return;
        previewOriginal = original;
        binding.compare.setSelected(original);
        if (!isPlayerAvailable()) return;
        player.previewAudioSetting(original);
        updateControls();
    }

    private int chipForPreset(int preset) {
        for (int[] item : getPresetItems()) if (item[0] == preset) return item[1];
        return View.NO_ID;
    }

    private int presetForChip(int chipId) {
        for (int[] item : getPresetItems()) if (item[1] == chipId) return item[0];
        return AudioEffectPreset.CUSTOM;
    }

    private int getAppliedPreset() {
        return AudioSetting.isEnabled() ? AudioSetting.getPreset() : AudioEffectPreset.OFF;
    }

    private int[][] getPresetItems() {
        return new int[][]{
                {AudioEffectPreset.OFF, binding.presetOriginal.getId()},
                {AudioEffectPreset.NATURAL, binding.presetNatural.getId()},
                {AudioEffectPreset.VOCAL, binding.presetVocal.getId()},
                {AudioEffectPreset.CINEMA, binding.presetCinema.getId()},
                {AudioEffectPreset.BASS, binding.presetBass.getId()},
                {AudioEffectPreset.TREBLE, binding.presetTreble.getId()},
                {AudioEffectPreset.POP, binding.presetPop.getId()},
                {AudioEffectPreset.ROCK, binding.presetRock.getId()},
                {AudioEffectPreset.DANCE, binding.presetDance.getId()},
                {AudioEffectPreset.ELECTRONIC, binding.presetElectronic.getId()},
                {AudioEffectPreset.HIPHOP, binding.presetHipHop.getId()},
                {AudioEffectPreset.JAZZ, binding.presetJazz.getId()},
                {AudioEffectPreset.CLASSICAL, binding.presetClassical.getId()},
                {AudioEffectPreset.CUSTOM, binding.presetCustom.getId()},
        };
    }

    private int chipForChannelMode(int mode) {
        int chip = binding.channelAuto.getId();
        if (mode == AudioChannelMode.STEREO) chip = binding.channelStereo.getId();
        else if (mode == AudioChannelMode.MONO) chip = binding.channelMono.getId();
        else if (mode == AudioChannelMode.REVERSE) chip = binding.channelReverse.getId();
        return chip;
    }

    private int channelModeForChip(int chipId) {
        int mode = AudioChannelMode.AUTO;
        if (chipId == binding.channelStereo.getId()) mode = AudioChannelMode.STEREO;
        else if (chipId == binding.channelMono.getId()) mode = AudioChannelMode.MONO;
        else if (chipId == binding.channelReverse.getId()) mode = AudioChannelMode.REVERSE;
        return mode;
    }

    private int getStepSize() {
        int range = bands.getMaxLevel() - bands.getMinLevel();
        return range > 0 && range % LEVEL_STEP == 0 ? LEVEL_STEP : 0;
    }

    private String formatFrequency(int milliHz) {
        int hz = milliHz / 1000;
        return hz >= 1000 ? String.format(Locale.getDefault(), "%.1f kHz", hz / 1000.0f) : String.format(Locale.getDefault(), "%d Hz", hz);
    }

    private String formatLevel(float milliBel) {
        return String.format(Locale.getDefault(), "%+.1f dB", milliBel / 100.0f);
    }

    private String formatPreamp(float attenuation) {
        return formatLevel(attenuation == 0.0f ? 0.0f : -attenuation);
    }

    private String formatPercent(float value) {
        return String.format(Locale.getDefault(), "%.0f%%", value);
    }

    private String formatStability(float value) {
        String[] levels = binding.getRoot().getResources().getStringArray(R.array.audio_effect_stability_levels);
        int index = Math.round(value / STABILITY_STEP);
        index = Math.clamp(index, 0, levels.length - 1);
        return levels[index];
    }

    private String formatBalance(float value) {
        int balance = Math.round(value);
        if (balance == 0) return binding.getRoot().getContext().getString(R.string.audio_effect_balance_center);
        if (balance < 0) return binding.getRoot().getContext().getString(R.string.audio_effect_balance_left, Math.abs(balance));
        return binding.getRoot().getContext().getString(R.string.audio_effect_balance_right, balance);
    }

    private String formatOffset(float value) {
        return String.format(Locale.getDefault(), "%+.1fs", value / 1000.0f);
    }

    private interface ValueFormatter {

        String format(float value);
    }

    private interface ValueSetter {

        void set(int value);
    }

    private interface BooleanSetter {

        void set(boolean value);
    }
}
