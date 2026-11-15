package com.fongmi.android.tv.ui.dialog;

import static com.fongmi.android.tv.ui.dialog.SettingPanelViews.applyEnabled;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.audio.AudioChannelMode;
import androidx.media3.exoplayer.ExoPlayer;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogAudioSettingBinding;
import com.fongmi.android.tv.databinding.ViewSettingSliderBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.effect.audio.AudioEffectBands;
import com.fongmi.android.tv.player.effect.audio.AudioEffectConfig;
import com.fongmi.android.tv.player.effect.audio.AudioEffectPreset;
import com.fongmi.android.tv.player.effect.audio.AudioPresetLevels;
import com.fongmi.android.tv.setting.AudioSetting;
import com.fongmi.android.tv.setting.OutputSetting;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.SliderUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.github.catvod.utils.Task;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.function.IntConsumer;

final class AudioSettingPanel implements Player.Listener {

    private static final int TAB_ADVANCED = 3;
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
    private static final int[][] PRESETS = {
            {AudioEffectPreset.OFF, R.id.presetOriginal},
            {AudioEffectPreset.NATURAL, R.id.presetNatural},
            {AudioEffectPreset.VOCAL, R.id.presetVocal},
            {AudioEffectPreset.CINEMA, R.id.presetCinema},
            {AudioEffectPreset.BASS, R.id.presetBass},
            {AudioEffectPreset.TREBLE, R.id.presetTreble},
            {AudioEffectPreset.POP, R.id.presetPop},
            {AudioEffectPreset.ROCK, R.id.presetRock},
            {AudioEffectPreset.DANCE, R.id.presetDance},
            {AudioEffectPreset.ELECTRONIC, R.id.presetElectronic},
            {AudioEffectPreset.HIPHOP, R.id.presetHipHop},
            {AudioEffectPreset.JAZZ, R.id.presetJazz},
            {AudioEffectPreset.CLASSICAL, R.id.presetClassical},
            {AudioEffectPreset.CUSTOM, R.id.presetCustom},
    };

    private final DialogAudioSettingBinding binding;
    private final PlayerManager player;
    @Nullable private final Player eventSource;
    @Nullable private Runnable stopObservingAudioOutput;
    private final List<ViewSettingSliderBinding> bandViews;
    private AudioEffectBands bands;
    private int currentTab;
    private int unsupportedReason;
    private boolean previewOriginal;
    private boolean released;

    AudioSettingPanel(DialogAudioSettingBinding binding, PlayerManager player) {
        this.binding = binding;
        this.player = player;
        this.eventSource = player.getPlayer();
        this.bandViews = new ArrayList<>();
        this.bands = AudioEffectBands.EMPTY;
    }

    void bind() {
        bands = getAudioBands();
        updatePresetCheck();
        bindSliders();
        bindChannelMode();
        bindLoudness();
        bindAudioPassThrough();
        SettingPanelViews.bindCompare(binding.compare, this::previewOriginal);
        bindReset();
        SettingPanelViews.bindPlaybackTabs(binding.tabGroup, getTabs(), this::showTab);
        updateControls();
        if (eventSource != null) eventSource.addListener(this);
        stopObservingAudioOutput = player.observeAudioOutput(this::updateAudioOutput);
    }

    void release() {
        released = true;
        if (eventSource != null) eventSource.removeListener(this);
        if (stopObservingAudioOutput != null) stopObservingAudioOutput.run();
        binding.tabGroup.clearOnButtonCheckedListeners();
        binding.audioPassThrough.setOnCheckedChangeListener(null);
        previewOriginal(false);
    }

    @Override
    public void onEvents(Player source, Player.Events events) {
        if (!events.containsAny(Player.EVENT_TRACKS_CHANGED, Player.EVENT_PLAYBACK_STATE_CHANGED)) return;
        updateAudioOutput();
    }

    private void updateAudioOutput() {
        if (eventSource != player.getPlayer()) return;
        if (isAudioPassThroughChanging() || (!canSetAudioSetting() && !isAudioOutputReady())) return;
        AudioEffectBands updatedBands = getAudioBands();
        if (bands != updatedBands) {
            bands = updatedBands;
            bindBands();
        }
        bindChannelMode();
        updateControls();
    }

    private void bindAudioPassThrough() {
        PlayerManager.AudioPassThroughChange change = player.getAudioPassThroughChange(eventSource);
        binding.audioPassThrough.setOnCheckedChangeListener(null);
        binding.audioPassThrough.setChecked(change == null ? OutputSetting.isAudioPassThrough() : change.enabled());
        binding.audioPassThrough.setOnCheckedChangeListener(this::onAudioPassThroughChanged);
        binding.audioPassThrough.setEnabled(change == null);
        if (change != null) observeAudioPassThrough(change.result());
    }

    private boolean isAudioPassThroughChanging() {
        return player.getAudioPassThroughChange(eventSource) != null;
    }

    private void onAudioPassThroughChanged(CompoundButton button, boolean enabled) {
        button.setEnabled(false);
        ListenableFuture<Void> change = player.setAudioPassThrough(eventSource, enabled);
        previewOriginal(false);
        observeAudioPassThrough(change);
    }

    private void observeAudioPassThrough(ListenableFuture<Void> change) {
        Futures.addCallback(change, Task.callback(result -> onAudioPassThroughFinished(null), this::onAudioPassThroughFinished), MoreExecutors.directExecutor());
    }

    private void onAudioPassThroughFinished(@Nullable Throwable error) {
        if (released) return;
        bindAudioPassThrough();
        updateAudioOutput();
        if (error != null && !(error instanceof CancellationException)) Notify.show(Notify.getError(R.string.player_audio_pass_through, error));
    }

    private void onPresetChecked(ChipGroup group, List<Integer> checkedIds) {
        if (checkedIds.isEmpty()) updatePresetCheck();
        else applyPreset(group, checkedIds.get(0));
    }

    private void applyPreset(ChipGroup group, int chipId) {
        SettingPanelViews.clearOtherPresets(getPresetGroups(), group, this::onPresetChecked);
        previewOriginal(false);
        setAudioSetting(presetForChip(chipId));
        bindBands();
        updateControls();
    }

    private void updatePresetCheck() {
        SettingPanelViews.checkPreset(getPresetGroups(), chipForPreset(getAppliedPreset()), this::onPresetChecked);
    }

    private ChipGroup[] getPresetGroups() {
        return new ChipGroup[]{binding.presetBasicGroup, binding.presetToneGroup, binding.presetGenreGroup, binding.presetCustomGroup};
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
        for (int i = 0; i < bands.getCount(); i++) addBandSlider(inflater, i, levels);
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

    private void bindLoudness() {
        binding.loudness.setOnCheckedChangeListener(null);
        binding.loudness.setChecked(AudioSetting.isLoudnessEnabled());
        binding.loudness.setOnCheckedChangeListener(this::onLoudnessChanged);
    }

    private void onLoudnessChanged(CompoundButton button, boolean enabled) {
        previewOriginal(false);
        AudioSetting.putLoudness(enabled);
        apply();
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
        bindLoudness();
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

    private void setupOptionSlider(ViewSettingSliderBinding item, int titleRes, int min, int max, int stepSize, int initial, LabelFormatter formatter, IntConsumer setter) {
        Slider slider = item.slider;
        item.title.setText(titleRes);
        slider.setContentDescription(item.title.getText());
        slider.clearOnChangeListeners();
        slider.setValueFrom(min);
        slider.setValueTo(max);
        slider.setStepSize(stepSize);
        slider.setLabelFormatter(formatter);
        SliderUtil.setValue(slider, initial);
        item.value.setText(formatter.getFormattedValue(slider.getValue()));
        slider.addOnChangeListener((source, value, fromUser) -> {
            if (!fromUser) return;
            previewOriginal(false);
            float snapped = SliderUtil.snap(source, value);
            item.value.setText(formatter.getFormattedValue(snapped));
            setter.accept(Math.round(snapped));
            apply();
        });
    }

    private void setupOffsetSlider() {
        Slider slider = binding.offset.slider;
        binding.offset.title.setText(R.string.audio_offset);
        slider.setContentDescription(binding.offset.title.getText());
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

    private void addBandSlider(LayoutInflater inflater, int index, short[] levels) {
        ViewSettingSliderBinding item = ViewSettingSliderBinding.inflate(inflater, binding.bands, false);
        setupBandSlider(item, index, levels[index]);
        bandViews.add(item);
        binding.bands.addView(item.getRoot());
    }

    private void setupBandSlider(ViewSettingSliderBinding item, int index, short initial) {
        Slider slider = item.slider;
        int stepSize = getStepSize();
        item.title.setText(formatFrequency(bands.getCenterFrequency(index)));
        slider.setContentDescription(item.title.getText());
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
        if (isAudioPassThroughChanging()) return;
        boolean supported = canSetAudioSetting();
        if (!supported && eventSource != null && !isAudioOutputReady()) return;
        supported = cancelUnsupportedPreview(supported);
        boolean hasBands = !bands.isEmpty();
        int channelCount = getAudioChannelCount();
        boolean active = AudioSetting.hasEffect(channelCount);
        boolean channelMix = AudioChannelMode.isAvailable(channelCount);
        boolean centerGain = AudioEffectConfig.isCenterGainAvailable(channelCount);
        boolean balance = channelMix && AudioChannelMode.canBalance(AudioSetting.getChannelMode());
        unsupportedReason = !supported && isAudioOutputReady() ? getUnsupportedText() : 0;
        updateUnsupported();
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

    private void updateUnsupported() {
        boolean visible = unsupportedReason != 0 && currentTab != TAB_ADVANCED;
        binding.unsupported.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) binding.unsupported.setText(unsupportedReason);
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
        bindLoudness();
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
        return !player.isReleased();
    }

    private boolean isAudioOutputReady() {
        if (eventSource == null || !isPlayerAvailable() || !SettingPanelViews.isPlaybackPrepared(eventSource)) return false;
        if (player.getVideoSettingEventSource() instanceof ExoPlayer exo) {
            if (exo.getAudioProcessingSupport() != ExoPlayer.AUDIO_PROCESSING_UNAVAILABLE) return true;
        } else if (player.getAudioSettingError() != 0) return true;
        Tracks tracks = eventSource.getCurrentTracks();
        return !tracks.isEmpty() && !tracks.isTypeSelected(C.TRACK_TYPE_AUDIO);
    }

    private void showTab(int index) {
        View[] roots = {binding.presetSection, binding.equalizerSection, binding.offsetSection, binding.advancedSection};
        MaterialButton[] tabs = getTabs();
        for (int i = 0; i < roots.length; i++) roots[i].setVisibility(index == i ? View.VISIBLE : View.GONE);
        currentTab = index;
        updateUnsupported();
        int focusId = tabs[index].getId();
        binding.reset.setNextFocusDownId(focusId);
        binding.compare.setNextFocusDownId(focusId);
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
        for (int[] item : PRESETS) if (item[0] == preset) return item[1];
        return View.NO_ID;
    }

    private int presetForChip(int chipId) {
        for (int[] item : PRESETS) if (item[1] == chipId) return item[0];
        return AudioEffectPreset.CUSTOM;
    }

    private int getAppliedPreset() {
        return AudioSetting.isEnabled() ? AudioSetting.getPreset() : AudioEffectPreset.OFF;
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

}
