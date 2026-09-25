package com.fongmi.android.tv.ui.dialog;

import static com.fongmi.android.tv.ui.dialog.SettingPanelViews.applyEnabled;

import android.view.View;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogVideoSettingBinding;
import com.fongmi.android.tv.databinding.ViewSettingSliderBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.effect.video.VideoEffectPreset;
import com.fongmi.android.tv.player.effect.video.VideoEffectProfile;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.VideoSetting;
import com.fongmi.android.tv.utils.SliderUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class VideoSettingPanel {

    private static final int[][] PRESETS = {
            {VideoEffectPreset.OFF, R.id.presetOriginal},
            {VideoEffectPreset.NATURAL, R.id.presetNatural},
            {VideoEffectPreset.VIVID, R.id.presetVivid},
            {VideoEffectPreset.CLEAR, R.id.presetClear},
            {VideoEffectPreset.BRIGHT, R.id.presetBright},
            {VideoEffectPreset.CINEMA, R.id.presetCinema},
            {VideoEffectPreset.SOFT, R.id.presetSoft},
            {VideoEffectPreset.WARM, R.id.presetWarm},
            {VideoEffectPreset.COOL, R.id.presetCool},
            {VideoEffectPreset.COMFORT, R.id.presetComfort},
            {VideoEffectPreset.ANIME, R.id.presetAnime},
            {VideoEffectPreset.SPORT, R.id.presetSport},
            {VideoEffectPreset.GAME, R.id.presetGame},
            {VideoEffectPreset.CUSTOM, R.id.presetCustom},
    };

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
        SettingPanelViews.bindTabs(binding.tabGroup, getTabs(), this::showTab);
        SettingPanelViews.bindCompare(binding.compare, this::previewOriginal);
        bindReset();
        showTab(0);
        updateControls();
        PlaybackDialogFocus.preferCheckedChips(binding.getRoot());
        PlaybackDialogFocus.selectFirstTab(binding.getRoot(), binding.tabGroup, binding.tabPreset);
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
        SettingPanelViews.clearOtherPresets(getPresetGroups(), group, this::onPresetChecked);
        previewOriginal(false);
        setVideoSetting(presetForChip(chipId));
        updateSliderValues(getDisplayProfile());
        updateControls();
    }

    private void updatePresetCheck() {
        SettingPanelViews.checkPreset(getPresetGroups(), chipForPreset(getAppliedPreset()), this::onPresetChecked);
    }

    private ChipGroup[] getPresetGroups() {
        return new ChipGroup[]{binding.presetBasicGroup, binding.presetBoostGroup, binding.presetToneGroup, binding.presetSceneGroup, binding.presetCustomGroup};
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
        return !player.isReleased();
    }

    private void showTab(int index) {
        View[] roots = {binding.presetSection, binding.adjustSection};
        MaterialButton[] tabs = getTabs();
        for (int i = 0; i < roots.length; i++) roots[i].setVisibility(index == i ? View.VISIBLE : View.GONE);
        currentTab = index;
        int focusId = tabs[index].getId();
        binding.reset.setNextFocusDownId(focusId);
        binding.compare.setNextFocusDownId(focusId);
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
        for (int[] item : PRESETS) if (item[0] == preset) return item[1];
        return View.NO_ID;
    }

    private int presetForChip(int chipId) {
        for (int[] item : PRESETS) if (item[1] == chipId) return item[0];
        return VideoEffectPreset.CUSTOM;
    }

    private int getAppliedPreset() {
        return VideoSetting.isEnabled() ? VideoSetting.getPreset() : VideoEffectPreset.OFF;
    }

    private String format(float value, String format) {
        return String.format(Locale.getDefault(), format, value);
    }
}
