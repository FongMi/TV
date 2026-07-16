package com.fongmi.android.tv.ui.dialog;

import android.annotation.SuppressLint;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogVideoSettingBinding;
import com.fongmi.android.tv.databinding.ViewSettingSliderBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.effect.video.VideoEffectPreset;
import com.fongmi.android.tv.player.effect.video.VideoEffectProfile;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.VideoSetting;
import com.fongmi.android.tv.utils.SliderUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class VideoSettingPanel {

    private final DialogVideoSettingBinding binding;
    private final PlayerManager player;
    private int currentTab;
    private boolean previewOriginal;

    VideoSettingPanel(DialogVideoSettingBinding binding, PlayerManager player) {
        this.binding = binding;
        this.player = player;
    }

    void bind() {
        updatePresetCheck();
        bindSliders();
        bindTabs();
        bindCompare();
        bindReset();
        showTab(0);
        updateControls();
        if (Util.isLeanback()) binding.tabPreset.requestFocus();
        binding.tabGroup.check(binding.tabPreset.getId());
    }

    void release() {
        previewOriginal(false);
    }

    private void onPresetChecked(ChipGroup group, List<Integer> checkedIds) {
        if (checkedIds.isEmpty()) updatePresetCheck();
        else applyPreset(group, checkedIds.get(0));
    }

    private void applyPreset(ChipGroup group, int chipId) {
        clearOtherPresetGroups(group);
        previewOriginal(false);
        setVideoSetting(presetForChip(chipId));
        updateSliderValues(getDisplayProfile());
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
        return new ChipGroup[]{binding.presetBasicGroup, binding.presetBoostGroup, binding.presetToneGroup, binding.presetSceneGroup, binding.presetCustomGroup};
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
        VideoEffectProfile profile = getDisplayProfile();
        setupSlider(binding.saturation, R.string.video_effect_saturation, VideoSetting.MIN_SATURATION, VideoSetting.MAX_SATURATION, 0.01f, profile.getSaturation(), "%.2f", VideoSetting::putSaturation);
        setupSlider(binding.contrast, R.string.video_effect_contrast, VideoSetting.MIN_CONTRAST, VideoSetting.MAX_CONTRAST, 0.01f, profile.getContrast(), "%.2f", VideoSetting::putContrast);
        setupSlider(binding.brightness, R.string.video_effect_brightness, VideoSetting.MIN_BRIGHTNESS, VideoSetting.MAX_BRIGHTNESS, 0.005f, profile.getBrightness(), "%+.3f", VideoSetting::putBrightness);
        setupSlider(binding.gamma, R.string.video_effect_gamma, VideoSetting.MIN_GAMMA, VideoSetting.MAX_GAMMA, 0.01f, profile.getGamma(), "%.2f", VideoSetting::putGamma);
        setupSlider(binding.hue, R.string.video_effect_hue, VideoSetting.MIN_HUE, VideoSetting.MAX_HUE, 1.0f, profile.getHue(), "%+.0f", VideoSetting::putHue);
        setupSlider(binding.temperature, R.string.video_effect_temperature, VideoSetting.MIN_TEMPERATURE, VideoSetting.MAX_TEMPERATURE, 1.0f, profile.getTemperature(), "%+.0f", VideoSetting::putTemperature);
        setupSlider(binding.sharpness, R.string.video_effect_sharpness, VideoSetting.MIN_SHARPNESS, VideoSetting.MAX_SHARPNESS, 0.01f, profile.getSharpness(), "%.2f", VideoSetting::putSharpness);
        setupSlider(binding.shadow, R.string.video_effect_shadow, VideoSetting.MIN_SHADOW, VideoSetting.MAX_SHADOW, 0.01f, profile.getShadowLift(), "%.2f", VideoSetting::putShadow);
    }

    private VideoEffectProfile getDisplayProfile() {
        return getProfileForPreset(VideoSetting.getPreset());
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
        if (currentTab == 0) resetPreset();
        else resetAdjust();
    }

    private void resetPreset() {
        VideoSetting.putPreset(VideoEffectPreset.OFF);
        syncControls();
        apply();
    }

    private void resetAdjust() {
        VideoSetting.putCustomProfile(VideoEffectProfile.off());
        if (VideoSetting.isEnabled()) VideoSetting.putPreset(VideoEffectPreset.CUSTOM);
        syncControls();
        apply();
    }

    private void resetAll() {
        previewOriginal(false);
        VideoSetting.reset();
        syncControls();
        apply();
    }

    private void setupSlider(ViewSettingSliderBinding item, int titleRes, float from, float to, float step, float initial, String format, Consumer<Float> setter) {
        item.title.setText(titleRes);
        Slider slider = item.slider;
        float clamped = SliderUtil.snap(initial, from, to, step);
        slider.clearOnChangeListeners();
        slider.setValueFrom(from);
        slider.setValueTo(to);
        slider.setStepSize(step);
        slider.setLabelFormatter(value -> format(value, format));
        SliderUtil.setValue(slider, clamped);
        item.value.setText(format(clamped, format));
        slider.addOnChangeListener((source, value, fromUser) -> {
            if (!fromUser) return;
            previewOriginal(false);
            float snapped = SliderUtil.snap(source, value);
            setter.accept(snapped);
            item.value.setText(format(snapped, format));
            switchToCustom();
            apply();
        });
    }

    private void updateSliderValues(VideoEffectProfile profile) {
        setSliderValue(binding.saturation, profile.getSaturation(), "%.2f");
        setSliderValue(binding.contrast, profile.getContrast(), "%.2f");
        setSliderValue(binding.brightness, profile.getBrightness(), "%+.3f");
        setSliderValue(binding.gamma, profile.getGamma(), "%.2f");
        setSliderValue(binding.hue, profile.getHue(), "%+.0f");
        setSliderValue(binding.temperature, profile.getTemperature(), "%+.0f");
        setSliderValue(binding.sharpness, profile.getSharpness(), "%.2f");
        setSliderValue(binding.shadow, profile.getShadowLift(), "%.2f");
    }

    private void setSliderValue(ViewSettingSliderBinding item, float value, String format) {
        float snapped = SliderUtil.snap(item.slider, value);
        SliderUtil.setValue(item.slider, snapped);
        item.value.setText(format(snapped, format));
    }

    private void switchToCustom() {
        if (VideoSetting.isEnabled() && VideoSetting.getPreset() == VideoEffectPreset.CUSTOM) return;
        VideoSetting.putCustomProfile(getCurrentProfile());
        VideoSetting.putPreset(VideoEffectPreset.CUSTOM);
        updatePresetCheck();
        updateControls();
    }

    private VideoEffectProfile getProfileForPreset(int preset) {
        return preset == VideoEffectPreset.CUSTOM ? VideoSetting.getCustomProfile() : VideoEffectProfile.of(preset);
    }

    private VideoEffectProfile getCurrentProfile() {
        VideoEffectProfile profile = getDisplayProfile();
        float shadow = isMpv() ? profile.getShadowLift() : binding.shadow.slider.getValue();
        float temperature = isMpv() ? profile.getTemperature() : binding.temperature.slider.getValue();
        float sharpness = supportsSharpness() ? binding.sharpness.slider.getValue() : profile.getSharpness();
        return VideoEffectProfile.custom(binding.saturation.slider.getValue(), binding.contrast.slider.getValue(), binding.brightness.slider.getValue(), sharpness, shadow, binding.gamma.slider.getValue(), binding.hue.slider.getValue(), temperature);
    }

    private void updateControls() {
        boolean mpv = isMpv();
        boolean sharpnessSupported = supportsSharpness();
        boolean supported = canSetVideoSetting();
        if (!supported) previewOriginal(false);
        boolean checked = VideoSetting.isEnabled();
        boolean enabled = supported && checked;
        updateUnsupported(supported);
        binding.temperature.getRoot().setVisibility(mpv ? View.GONE : View.VISIBLE);
        binding.sharpness.getRoot().setVisibility(sharpnessSupported ? View.VISIBLE : View.GONE);
        binding.shadow.getRoot().setVisibility(mpv ? View.GONE : View.VISIBLE);
        applyEnabled(binding.compare, enabled);
        applyEnabled(binding.presetSection, supported);
        applyEnabled(binding.saturation.getRoot(), enabled);
        applyEnabled(binding.contrast.getRoot(), enabled);
        applyEnabled(binding.brightness.getRoot(), enabled);
        applyEnabled(binding.gamma.getRoot(), enabled);
        applyEnabled(binding.hue.getRoot(), enabled);
        applyEnabled(binding.temperature.getRoot(), enabled && !mpv);
        applyEnabled(binding.sharpness.getRoot(), enabled && sharpnessSupported);
        applyEnabled(binding.shadow.getRoot(), enabled && !mpv);
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
        if (isPlayerAvailable()) player.refreshVideoSetting();
    }

    private void syncControls() {
        updatePresetCheck();
        bindSliders();
        updateControls();
    }

    private void setVideoSetting(int preset) {
        if (!isPlayerAvailable()) VideoSetting.putPreset(preset);
        else player.setVideoSetting(preset);
    }

    private boolean canSetVideoSetting() {
        return isPlayerAvailable() && player.canSetVideoSetting();
    }

    private boolean isMpv() {
        return isPlayerAvailable() && player.getEngine() == PlayerSetting.ENGINE_MPV;
    }

    private boolean supportsSharpness() {
        return !isMpv() || player.supportsVideoSharpness();
    }

    private int getUnsupportedText() {
        int reason = isPlayerAvailable() ? player.getVideoSettingError() : 0;
        return reason == 0 ? R.string.error_video_effect_unsupported : reason;
    }

    private boolean isPlayerAvailable() {
        return player != null && !player.isReleased();
    }

    private void showTab(int index) {
        View[] roots = {binding.presetSection, binding.adjustSection};
        MaterialButton[] tabs = getTabs();
        for (int i = 0; i < roots.length; i++) roots[i].setVisibility(index == i ? View.VISIBLE : View.GONE);
        binding.reset.setNextFocusDownId(tabs[currentTab = index].getId());
    }

    private MaterialButton[] getTabs() {
        return new MaterialButton[]{binding.tabPreset, binding.tabAdjust};
    }

    private void previewOriginal(boolean original) {
        if (previewOriginal == original) return;
        previewOriginal = original;
        binding.compare.setSelected(original);
        if (!isPlayerAvailable()) return;
        player.previewVideoSetting(original);
    }

    private int chipForPreset(int preset) {
        for (int[] item : getPresetItems()) if (item[0] == preset) return item[1];
        return View.NO_ID;
    }

    private int presetForChip(int chipId) {
        for (int[] item : getPresetItems()) if (item[1] == chipId) return item[0];
        return VideoEffectPreset.CUSTOM;
    }

    private int getAppliedPreset() {
        return VideoSetting.isEnabled() ? VideoSetting.getPreset() : VideoEffectPreset.OFF;
    }

    private int[][] getPresetItems() {
        return new int[][]{
                {VideoEffectPreset.OFF, binding.presetOriginal.getId()},
                {VideoEffectPreset.NATURAL, binding.presetNatural.getId()},
                {VideoEffectPreset.VIVID, binding.presetVivid.getId()},
                {VideoEffectPreset.CLEAR, binding.presetClear.getId()},
                {VideoEffectPreset.BRIGHT, binding.presetBright.getId()},
                {VideoEffectPreset.CINEMA, binding.presetCinema.getId()},
                {VideoEffectPreset.SOFT, binding.presetSoft.getId()},
                {VideoEffectPreset.WARM, binding.presetWarm.getId()},
                {VideoEffectPreset.COOL, binding.presetCool.getId()},
                {VideoEffectPreset.COMFORT, binding.presetComfort.getId()},
                {VideoEffectPreset.ANIME, binding.presetAnime.getId()},
                {VideoEffectPreset.SPORT, binding.presetSport.getId()},
                {VideoEffectPreset.GAME, binding.presetGame.getId()},
                {VideoEffectPreset.CUSTOM, binding.presetCustom.getId()},
        };
    }

    private String format(float value, String format) {
        return String.format(Locale.getDefault(), format, value);
    }
}
