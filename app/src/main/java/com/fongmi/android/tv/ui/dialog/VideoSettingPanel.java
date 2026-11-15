package com.fongmi.android.tv.ui.dialog;

import static com.fongmi.android.tv.ui.dialog.SettingPanelViews.applyEnabled;

import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.Nullable;
import androidx.media3.common.DolbyVisionOutputPolicy;
import androidx.media3.common.Player;
import androidx.media3.mpvplayer.MpvPlayer;
import androidx.media3.mpvplayer.MpvPlayer.VideoOutputOption;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogVideoSettingBinding;
import com.fongmi.android.tv.databinding.ViewSettingSliderBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.effect.video.VideoEffectPreset;
import com.fongmi.android.tv.player.effect.video.VideoEffectProfile;
import com.fongmi.android.tv.player.mpv.MpvUtil;
import com.fongmi.android.tv.setting.OutputSetting;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.VideoSetting;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.SliderUtil;
import com.github.catvod.utils.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

final class VideoSettingPanel implements Player.Listener {

    private static final int TAB_ADVANCED = 2;
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
    @Nullable private final Player eventSource;
    private final MotionSettingPanel motionPanel;
    private final Runnable mpvMotionListener = this::updateMpvControls;
    private int currentTab;
    private boolean previewOriginal;
    private boolean effectsSupported;
    private int effectsError;
    private boolean released;

    VideoSettingPanel(DialogVideoSettingBinding binding, PlayerManager player) {
        this.binding = binding;
        this.player = player;
        this.eventSource = player.getVideoSettingEventSource();
        this.motionPanel = new MotionSettingPanel(binding.motion, eventSource, player.getExoMotionController());
    }

    void bind() {
        motionPanel.bind();
        updatePresetCheck();
        bindSliders();
        bindAdvanced();
        SettingPanelViews.bindCompare(binding.compare, this::previewOriginal);
        bindReset();
        SettingPanelViews.bindPlaybackTabs(binding.tabGroup, getTabs(), this::showTab);
        updateControls();
        if (eventSource != null) eventSource.addListener(this);
    }

    void release() {
        released = true;
        if (eventSource != null) eventSource.removeListener(this);
        if (eventSource instanceof MpvPlayer mpv) mpv.removeMotionInterpolationListener(mpvMotionListener);
        motionPanel.release();
        binding.tabGroup.clearOnButtonCheckedListeners();
        binding.tunnel.setOnCheckedChangeListener(null);
        binding.dolbyVisionGroup.setOnCheckedStateChangeListener(null);
        binding.mpvOutput.mpvGpuNext.setOnClickListener(null);
        binding.mpvOutput.mpvVulkan.setOnClickListener(null);
        previewOriginal(false);
    }

    @Override
    public void onEvents(Player source, Player.Events events) {
        if (source != player.getVideoSettingEventSource()) return;
        if (events.containsAny(Player.EVENT_TRACKS_CHANGED, Player.EVENT_RENDERED_FIRST_FRAME, Player.EVENT_PLAYBACK_STATE_CHANGED)) {
            updateControls();
            updateMpvOutput();
            motionPanel.updateMotion();
        }
    }

    private void bindAdvanced() {
        bindTunneling();
        bindDolbyVision();
        bindMpvOutput();
    }

    private void bindTunneling() {
        boolean tunnelingAvailable = player.canSetTunneling();
        binding.tunnelRow.setVisibility(tunnelingAvailable ? View.VISIBLE : View.GONE);
        binding.tunnel.setChecked(OutputSetting.isTunnelingEnabled());
        if (tunnelingAvailable) binding.tunnel.setOnCheckedChangeListener(this::onTunnelChanged);
    }

    private void bindDolbyVision() {
        String[] modes = ResUtil.getStringArray(R.array.select_dolby_vision_output);
        binding.dolbyVisionAuto.setText(modes[DolbyVisionOutputPolicy.AUTO]);
        binding.dolbyVisionOn.setText(modes[DolbyVisionOutputPolicy.ASSUME_SUPPORTED]);
        binding.dolbyVisionOff.setText(modes[DolbyVisionOutputPolicy.ASSUME_UNSUPPORTED]);
        updateDolbyVision();
        binding.dolbyVisionGroup.setOnCheckedStateChangeListener(this::onDolbyVisionChecked);
    }

    private void updateDolbyVision() {
        Chip chip = switch (getDisplayedDolbyVisionPolicy()) {
            case DolbyVisionOutputPolicy.ASSUME_SUPPORTED -> binding.dolbyVisionOn;
            case DolbyVisionOutputPolicy.ASSUME_UNSUPPORTED -> binding.dolbyVisionOff;
            default -> binding.dolbyVisionAuto;
        };
        binding.dolbyVisionGroup.check(chip.getId());
    }

    @DolbyVisionOutputPolicy.Mode
    private int getDisplayedDolbyVisionPolicy() {
        Integer pending = eventSource instanceof MpvPlayer mpv ? mpv.getPendingDolbyVisionOutputPolicy() : null;
        return pending == null ? OutputSetting.getDolbyVisionOutputPolicy() : pending;
    }

    private void onTunnelChanged(CompoundButton button, boolean enabled) {
        player.setTunnelingEnabled(enabled);
    }

    private void onDolbyVisionChecked(ChipGroup group, List<Integer> checkedIds) {
        if (checkedIds.isEmpty()) return;
        int chipId = checkedIds.get(0);
        int mode = DolbyVisionOutputPolicy.AUTO;
        if (chipId == R.id.dolbyVisionOn) mode = DolbyVisionOutputPolicy.ASSUME_SUPPORTED;
        else if (chipId == R.id.dolbyVisionOff) mode = DolbyVisionOutputPolicy.ASSUME_UNSUPPORTED;
        if (mode == getDisplayedDolbyVisionPolicy()) return;
        Futures.addCallback(player.setDolbyVisionOutputPolicy(eventSource, mode), Task.callback(
                result -> onDolbyVisionFinished(null), this::onDolbyVisionFinished), MoreExecutors.directExecutor());
    }

    private void onDolbyVisionFinished(@Nullable Throwable error) {
        if (released) return;
        updateMpvControls();
        if (error != null && !(error instanceof CancellationException)) Notify.show(Notify.getError(R.string.player_dolby_vision_output, error));
    }

    private void bindMpvOutput() {
        updateMpvOutput();
        if (!(eventSource instanceof MpvPlayer mpv)) return;
        binding.mpvOutput.mpvGpuNext.setOnClickListener(this::onMpvGpuNextClicked);
        binding.mpvOutput.mpvVulkan.setOnClickListener(this::onMpvVulkanClicked);
        mpv.addMotionInterpolationListener(mpvMotionListener);
    }

    private void updateMpvControls() {
        updateDolbyVision();
        updateMpvOutput();
        updateControls();
    }

    private void updateMpvOutput() {
        Boolean gpuNext = player.getPendingMpvVideoOutput(eventSource, VideoOutputOption.GPU_NEXT);
        Boolean vulkan = player.getPendingMpvVideoOutput(eventSource, VideoOutputOption.VULKAN);
        boolean switchingOutput = gpuNext != null || vulkan != null;
        boolean motionPending = !switchingOutput && isMpvMotionPending();
        boolean pending = switchingOutput || motionPending;
        if (!pending && isMpvOutputPreparing()) return;
        boolean available = switchingOutput || isMpvOutputAvailable();
        boolean visible = available || (pending && binding.mpvOutput.getRoot().getVisibility() == View.VISIBLE);
        binding.mpvOutput.getRoot().setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) return;
        boolean canChange = canChangeMpvOutput();
        updateMpvOutputSwitch(binding.mpvOutput.mpvGpuNext, gpuNext, OutputSetting.isMpvGpuNext(), canChange);
        updateMpvOutputSwitch(binding.mpvOutput.mpvVulkan, vulkan, OutputSetting.isMpvVulkan(), canChange);
        binding.mpvOutput.mpvVulkanRow.setVisibility(MpvUtil.isVulkanSupported() ? View.VISIBLE : View.GONE);
    }

    private void updateMpvOutputSwitch(CompoundButton button, @Nullable Boolean pending, boolean enabled, boolean canChange) {
        button.setChecked(pending != null ? pending : enabled);
        if (canChange || pending != null) button.setEnabled(pending == null);
        button.setClickable(canChange && pending == null);
    }

    private boolean isMpvOutputAvailable() {
        return !player.isReleased() && eventSource instanceof MpvPlayer mpv && mpv.canSetVideoOutput() && !MpvUtil.isMotionEnabled();
    }

    private boolean canChangeMpvOutput() {
        return !player.isReleased() && eventSource instanceof MpvPlayer mpv && mpv.canSetVideoOutputOption() && !MpvUtil.isMotionEnabled();
    }

    private boolean isMpvGpuOutputPending() {
        return player.getPendingMpvVideoOutput(eventSource, VideoOutputOption.GPU_NEXT) != null || player.getPendingMpvVideoOutput(eventSource, VideoOutputOption.VULKAN) != null;
    }

    private boolean isMpvMotionPending() {
        return eventSource instanceof MpvPlayer mpv && mpv.isMotionInterpolationUpdatePending();
    }

    private boolean isMpvOutputPreparing() {
        return eventSource instanceof MpvPlayer mpv && mpv.getPlaybackState() == Player.STATE_BUFFERING && mpv.getVideoEffectsSupport() == MpvPlayer.VIDEO_EFFECTS_UNAVAILABLE;
    }

    private void onMpvGpuNextClicked(View view) {
        setMpvVideoOutput(VideoOutputOption.GPU_NEXT, binding.mpvOutput.mpvGpuNext.isChecked());
    }

    private void onMpvVulkanClicked(View view) {
        setMpvVideoOutput(VideoOutputOption.VULKAN, binding.mpvOutput.mpvVulkan.isChecked());
    }

    private void setMpvVideoOutput(VideoOutputOption option, boolean enabled) {
        if (!canChangeMpvOutput() || player.getPendingMpvVideoOutput(eventSource, option) != null) {
            updateMpvOutput();
            return;
        }
        Futures.addCallback(player.setMpvVideoOutput(eventSource, option, enabled), Task.callback(
                result -> onMpvOutputFinished(null), this::onMpvOutputFinished), MoreExecutors.directExecutor());
        updateMpvOutput();
    }

    private void onMpvOutputFinished(@Nullable Throwable error) {
        if (released) return;
        updateMpvControls();
        if (error != null && !(error instanceof CancellationException)) Notify.show(Notify.getError(R.string.mpv_script_error, error));
    }

    private void onPresetChecked(ChipGroup group, List<Integer> checkedIds) {
        if (checkedIds.isEmpty()) updatePresetCheck();
        else applyPreset(group, checkedIds.get(0));
    }

    private void applyPreset(ChipGroup group, int chipId) {
        SettingPanelViews.clearOtherPresets(getPresetGroups(), group, this::onPresetChecked);
        previewOriginal(false);
        setVideoSetting(presetForChip(chipId));
        updateSliderValues(VideoSetting.getProfile());
        updateControls();
    }

    private void updatePresetCheck() {
        SettingPanelViews.checkPreset(getPresetGroups(), chipForPreset(getAppliedPreset()), this::onPresetChecked);
    }

    private ChipGroup[] getPresetGroups() {
        return new ChipGroup[]{binding.presetBasicGroup, binding.presetBoostGroup, binding.presetToneGroup, binding.presetSceneGroup, binding.presetCustomGroup};
    }

    private void bindSliders() {
        VideoEffectProfile profile = VideoSetting.getProfile();
        setupSlider(binding.saturation, R.string.video_effect_saturation, VideoSetting.MIN_SATURATION, VideoSetting.MAX_SATURATION, 0.01f, profile.getSaturation(), "%.2f", VideoSetting::putSaturation);
        setupSlider(binding.contrast, R.string.video_effect_contrast, VideoSetting.MIN_CONTRAST, VideoSetting.MAX_CONTRAST, 0.01f, profile.getContrast(), "%.2f", VideoSetting::putContrast);
        setupSlider(binding.brightness, R.string.video_effect_brightness, VideoSetting.MIN_BRIGHTNESS, VideoSetting.MAX_BRIGHTNESS, 0.005f, profile.getBrightness(), "%+.3f", VideoSetting::putBrightness);
        setupSlider(binding.gamma, R.string.video_effect_gamma, VideoSetting.MIN_GAMMA, VideoSetting.MAX_GAMMA, 0.01f, profile.getGamma(), "%.2f", VideoSetting::putGamma);
        setupSlider(binding.hue, R.string.video_effect_hue, VideoSetting.MIN_HUE, VideoSetting.MAX_HUE, 1.0f, profile.getHue(), "%+.0f", VideoSetting::putHue);
        setupSlider(binding.temperature, R.string.video_effect_temperature, VideoSetting.MIN_TEMPERATURE, VideoSetting.MAX_TEMPERATURE, 1.0f, profile.getTemperature(), "%+.0f", VideoSetting::putTemperature);
        setupSlider(binding.sharpness, R.string.video_effect_sharpness, VideoSetting.MIN_SHARPNESS, VideoSetting.MAX_SHARPNESS, 0.01f, profile.getSharpness(), "%.2f", VideoSetting::putSharpness);
        setupSlider(binding.shadow, R.string.video_effect_shadow, VideoSetting.MIN_SHADOW, VideoSetting.MAX_SHADOW, 0.01f, profile.getShadowLift(), "%.2f", VideoSetting::putShadow);
    }

    private void bindReset() {
        binding.reset.setOnClickListener(this::onReset);
        binding.reset.setOnLongClickListener(this::onResetLong);
    }

    private boolean onResetLong(View view) {
        resetAll();
        return true;
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
        slider.setContentDescription(item.title.getText());
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

    private VideoEffectProfile getCurrentProfile() {
        VideoEffectProfile profile = VideoSetting.getProfile();
        float shadow = isMpv() ? profile.getShadowLift() : binding.shadow.slider.getValue();
        float temperature = isMpv() ? profile.getTemperature() : binding.temperature.slider.getValue();
        float sharpness = supportsSharpness() ? binding.sharpness.slider.getValue() : profile.getSharpness();
        return VideoEffectProfile.custom(binding.saturation.slider.getValue(), binding.contrast.slider.getValue(), binding.brightness.slider.getValue(), sharpness, shadow, binding.gamma.slider.getValue(), binding.hue.slider.getValue(), temperature);
    }

    private void updateControls() {
        if (isMpvGpuOutputPending() || isMpvMotionPending() || isMpvOutputPreparing()) return;
        boolean mpv = isMpv();
        boolean sharpnessSupported = supportsSharpness();
        effectsSupported = canSetVideoSetting();
        if (!effectsSupported) previewOriginal(false);
        effectsError = effectsSupported ? 0 : getUnsupportedText();
        boolean enabled = effectsSupported && VideoSetting.isEnabled();
        updateEffectActions();
        binding.temperature.getRoot().setVisibility(mpv ? View.GONE : View.VISIBLE);
        binding.sharpness.getRoot().setVisibility(sharpnessSupported ? View.VISIBLE : View.GONE);
        binding.shadow.getRoot().setVisibility(mpv ? View.GONE : View.VISIBLE);
        applyEnabled(binding.presetSection, effectsSupported);
        applyEnabled(binding.saturation.getRoot(), enabled);
        applyEnabled(binding.contrast.getRoot(), enabled);
        applyEnabled(binding.brightness.getRoot(), enabled);
        applyEnabled(binding.gamma.getRoot(), enabled);
        applyEnabled(binding.hue.getRoot(), enabled);
        applyEnabled(binding.temperature.getRoot(), enabled && !mpv);
        applyEnabled(binding.sharpness.getRoot(), enabled && sharpnessSupported);
        applyEnabled(binding.shadow.getRoot(), enabled && !mpv);
    }

    private void updateEffectActions() {
        boolean effectsTab = currentTab != TAB_ADVANCED;
        boolean visible = effectsError != 0 && effectsTab;
        applyEnabled(binding.compare, effectsSupported && VideoSetting.isEnabled() && effectsTab);
        applyEnabled(binding.reset, effectsTab);
        binding.unsupported.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) binding.unsupported.setText(effectsError);
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
        View[] roots = {binding.presetSection, binding.adjustSection, binding.advancedSection};
        MaterialButton[] tabs = getTabs();
        for (int i = 0; i < roots.length; i++) roots[i].setVisibility(index == i ? View.VISIBLE : View.GONE);
        currentTab = index;
        updateEffectActions();
        int focusId = tabs[index].getId();
        binding.reset.setNextFocusDownId(focusId);
        binding.compare.setNextFocusDownId(focusId);
    }

    private MaterialButton[] getTabs() {
        return new MaterialButton[]{binding.tabPreset, binding.tabAdjust, binding.tabAdvanced};
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
