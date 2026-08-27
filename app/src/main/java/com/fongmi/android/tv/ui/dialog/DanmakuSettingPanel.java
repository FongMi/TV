package com.fongmi.android.tv.ui.dialog;

import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.media3.ui.danmaku.DanmakuConfig;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogDanmakuSettingBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.subtitle.ExternalFont;
import com.fongmi.android.tv.setting.DanmakuSetting;
import com.fongmi.android.tv.utils.SliderUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongConsumer;

final class DanmakuSettingPanel {

    private final DialogDanmakuSettingBinding binding;
    private final PlayerManager player;
    private final ExternalFontSelector fontSelector;
    private int currentTab;

    DanmakuSettingPanel(DialogDanmakuSettingBinding binding, PlayerManager player, ExternalFontSelector fontSelector) {
        this.binding = binding;
        this.player = player;
        this.fontSelector = fontSelector;
    }

    void bind() {
        bindAppearance();
        bindTiming();
        bindDensity();
        bindDisplay();
        bindTabs();
        bindReset();
        showTab(0);
        if (Util.isLeanback()) binding.tabAppearance.requestFocus();
        binding.tabGroup.check(binding.tabAppearance.getId());
    }

    void onFontSelected(@Nullable ExternalFont.Item font) {
        DanmakuSetting.putFont(font);
        applyConfig();
    }

    private void bindAppearance() {
        var appearance = binding.appearance;
        bindFont();
        setupSwitch(appearance.textBoldSwitch, DanmakuSetting.isTextBold(), DanmakuSetting::putTextBold);
        setupFloat(appearance.textSizeSlider, appearance.textSizeValue, DanmakuSetting.getTextScale(), "%.1f", DanmakuSetting::putTextScale);
        setupFloat(appearance.alphaSlider, appearance.alphaValue, DanmakuSetting.getTransparency(), "%.2f", DanmakuSetting::putTransparency);
        setupFloat(appearance.shadowAlphaSlider, appearance.shadowAlphaValue, DanmakuSetting.getShadowTransparency(), "%.2f", DanmakuSetting::putShadowTransparency);
        setupFloat(appearance.strokeWidthSlider, appearance.strokeWidthValue, DanmakuSetting.getStrokeWidthMultiplier(), "%.2f", DanmakuSetting::putStrokeWidthMultiplier);
        setupFloat(appearance.projectionOffsetXSlider, appearance.projectionOffsetXValue, DanmakuSetting.getProjectionOffsetX(), "%.2f", DanmakuSetting::putProjectionOffsetX);
        setupFloat(appearance.projectionOffsetYSlider, appearance.projectionOffsetYValue, DanmakuSetting.getProjectionOffsetY(), "%.2f", DanmakuSetting::putProjectionOffsetY);
        setupFloat(appearance.projectionAlphaSlider, appearance.projectionAlphaValue, DanmakuSetting.getProjectionTransparency(), "%.2f", DanmakuSetting::putProjectionTransparency);
        setupChip(appearance.styleChipGroup, DanmakuSetting.getStyleMode(), this::styleChipForMode, this::styleModeForChip, this::onStyleModeChanged);
        setupChip(appearance.colorChipGroup, DanmakuSetting.getColorMode(), this::colorChipForMode, this::colorModeForChip, this::onColorModeChanged);
        updateStyleSubSettings(DanmakuSetting.getStyleMode());
        updateColorOverrideHint(DanmakuSetting.getColorMode());
    }

    private void bindFont() {
        fontSelector.bind(binding.appearance.fontGroup, DanmakuSetting.getFont());
    }

    private boolean isPlayerAvailable() {
        return player != null && !player.isReleased();
    }

    private void bindTiming() {
        var timing = binding.timing;
        setupMs(timing.timeOffsetSlider, timing.timeOffsetValue, DanmakuSetting.getTimeOffsetMs(), DanmakuSetting::putTimeOffsetMs);
        setupMs(timing.durationSlider, timing.durationValue, DanmakuSetting.getDurationMs(), DanmakuSetting::putDurationMs);
        setupMs(timing.fixedDurationSlider, timing.fixedDurationValue, DanmakuSetting.getFixedDurationMs(), DanmakuSetting::putFixedDurationMs);
    }

    private void bindDensity() {
        var density = binding.density;
        setupInt(density.maxOnScreenSlider, density.maxOnScreenValue, DanmakuSetting.getMaxOnScreen(), String::valueOf, DanmakuSetting::putMaxOnScreen);
        setupFloat(density.scrollAreaSlider, density.scrollAreaValue, DanmakuSetting.getScrollAreaRatio(), "%.2f", DanmakuSetting::putScrollAreaRatio);
        setupFloat(density.scrollGapSlider, density.scrollGapValue, DanmakuSetting.getScrollGapRatio(), "%.1f", DanmakuSetting::putScrollGapRatio);
        setupFloat(density.lineSpacingSlider, density.lineSpacingValue, DanmakuSetting.getLineSpacing(), "%.1f", DanmakuSetting::putLineSpacing);
        setupInt(density.maxScrollLinesSlider, density.maxScrollLinesValue, DanmakuSetting.getMaxScrollLines(), this::linesText, DanmakuSetting::putMaxScrollLines);
        setupInt(density.maxTopLinesSlider, density.maxTopLinesValue, DanmakuSetting.getMaxTopLines(), this::linesText, DanmakuSetting::putMaxTopLines);
        setupInt(density.maxBottomLinesSlider, density.maxBottomLinesValue, DanmakuSetting.getMaxBottomLines(), this::linesText, DanmakuSetting::putMaxBottomLines);
        updateDependentControls();
    }

    private void bindDisplay() {
        var display = binding.display;
        setupSwitch(display.showScrollSwitch, DanmakuSetting.isShowScroll(), DanmakuSetting::putShowScroll, this::updateDependentControls);
        setupSwitch(display.showTopSwitch, DanmakuSetting.isShowTop(), DanmakuSetting::putShowTop, this::updateDependentControls);
        setupSwitch(display.showBottomSwitch, DanmakuSetting.isShowBottom(), DanmakuSetting::putShowBottom, this::updateDependentControls);
        setupSwitch(display.showReverseSwitch, DanmakuSetting.isShowReverse(), DanmakuSetting::putShowReverse, this::updateDependentControls);
        setupSwitch(display.showPositionedSwitch, DanmakuSetting.isShowPositioned(), DanmakuSetting::putShowPositioned, null);
        setupSwitch(display.showSubtitleSwitch, DanmakuSetting.isShowSubtitle(), DanmakuSetting::putShowSubtitle, null);
        setupSwitch(display.showSpecialSwitch, DanmakuSetting.isShowSpecial(), DanmakuSetting::putShowSpecial, null);
    }

    private void bindTabs() {
        MaterialButton[] tabs = {binding.tabAppearance, binding.tabTiming, binding.tabDensity, binding.tabDisplay};
        for (MaterialButton tab : tabs) checkOnFocus(tab);
        binding.tabGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            for (int i = 0; i < tabs.length; i++) if (checkedId == tabs[i].getId()) showTab(i);
        });
    }

    private void checkOnFocus(MaterialButton button) {
        if (!Util.isLeanback()) return;
        button.setOnFocusChangeListener((v, focused) -> {
            if (focused) binding.tabGroup.check(button.getId());
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
        switch (currentTab) {
            case 0:
                DanmakuSetting.resetAppearance();
                bindAppearance();
                applyConfig();
                break;
            case 1:
                DanmakuSetting.resetTiming();
                bindTiming();
                applyConfig();
                break;
            case 2:
                DanmakuSetting.resetDensity();
                bindDensity();
                applyConfig();
                break;
            case 3:
                DanmakuSetting.resetDisplay();
                bindDisplay();
                updateDependentControls();
                applyConfig();
                break;
        }
    }

    private void showTab(int index) {
        View[] roots = {binding.appearance.getRoot(), binding.timing.getRoot(), binding.density.getRoot(), binding.display.getRoot()};
        MaterialButton[] tabs = {binding.tabAppearance, binding.tabTiming, binding.tabDensity, binding.tabDisplay};
        for (int i = 0; i < roots.length; i++) roots[i].setVisibility(visibleIf(index == i));
        binding.reset.setNextFocusDownId(tabs[currentTab = index].getId());
    }

    private void resetAll() {
        DanmakuSetting.resetAppearance();
        DanmakuSetting.resetTiming();
        DanmakuSetting.resetDensity();
        DanmakuSetting.resetDisplay();
        bindAppearance();
        bindTiming();
        bindDensity();
        bindDisplay();
        updateDependentControls();
        applyConfig();
    }

    private void updateStyleSubSettings(int mode) {
        var appearance = binding.appearance;
        applyVisible(mode == DanmakuConfig.STYLE_SHADOW, appearance.shadowAlphaRow, appearance.shadowAlphaSlider);
        applyVisible(mode == DanmakuConfig.STYLE_STROKE, appearance.strokeWidthRow, appearance.strokeWidthSlider);
        applyVisible(mode == DanmakuConfig.STYLE_PROJECTION, appearance.projectionOffsetXRow, appearance.projectionOffsetXSlider, appearance.projectionOffsetYRow, appearance.projectionOffsetYSlider, appearance.projectionAlphaRow, appearance.projectionAlphaSlider);
    }

    private void applyVisible(boolean visible, View... views) {
        int visibility = visibleIf(visible);
        for (View view : views) view.setVisibility(visibility);
    }

    private void updateColorOverrideHint(int mode) {
        binding.appearance.colorOverrideHint.setVisibility(visibleIf(mode != DanmakuConfig.COLOR_MODE_DEFAULT));
    }

    private void onStyleModeChanged(int mode) {
        DanmakuSetting.putStyleMode(mode);
        updateStyleSubSettings(mode);
    }

    private void onColorModeChanged(int mode) {
        DanmakuSetting.putColorMode(mode);
        updateColorOverrideHint(mode);
    }

    private void updateDependentControls() {
        var density = binding.density;
        applyEnabled(density.maxScrollLinesRow, density.maxScrollLinesSlider, DanmakuSetting.isShowScroll() || DanmakuSetting.isShowReverse());
        applyEnabled(density.maxTopLinesRow, density.maxTopLinesSlider, DanmakuSetting.isShowTop());
        applyEnabled(density.maxBottomLinesRow, density.maxBottomLinesSlider, DanmakuSetting.isShowBottom());
    }

    private void applyEnabled(View row, Slider slider, boolean enabled) {
        row.setAlpha(enabled ? 1f : 0.38f);
        slider.setEnabled(enabled);
    }

    private void applyConfig() {
        if (isPlayerAvailable()) player.setDanmakuConfig(DanmakuSetting.getConfig());
    }

    private int styleChipForMode(int mode) {
        var appearance = binding.appearance;
        if (mode == DanmakuConfig.STYLE_NONE) return appearance.styleNone.getId();
        if (mode == DanmakuConfig.STYLE_SHADOW) return appearance.styleShadow.getId();
        if (mode == DanmakuConfig.STYLE_PROJECTION) return appearance.styleProjection.getId();
        return appearance.styleStroke.getId();
    }

    private int styleModeForChip(int chipId) {
        var appearance = binding.appearance;
        if (chipId == appearance.styleNone.getId()) return DanmakuConfig.STYLE_NONE;
        if (chipId == appearance.styleShadow.getId()) return DanmakuConfig.STYLE_SHADOW;
        if (chipId == appearance.styleProjection.getId()) return DanmakuConfig.STYLE_PROJECTION;
        return DanmakuConfig.STYLE_STROKE;
    }

    private int colorChipForMode(int mode) {
        var appearance = binding.appearance;
        if (mode == DanmakuConfig.COLOR_MODE_COLORFUL) return appearance.colorColorful.getId();
        if (mode == DanmakuConfig.COLOR_MODE_GRADIENT) return appearance.colorGradient.getId();
        return appearance.colorDefault.getId();
    }

    private int colorModeForChip(int chipId) {
        var appearance = binding.appearance;
        if (chipId == appearance.colorColorful.getId()) return DanmakuConfig.COLOR_MODE_COLORFUL;
        if (chipId == appearance.colorGradient.getId()) return DanmakuConfig.COLOR_MODE_GRADIENT;
        return DanmakuConfig.COLOR_MODE_DEFAULT;
    }

    private void setupFloat(Slider slider, TextView label, float value, String format, FloatSetter setter) {
        setupSlider(slider, label, value, sliderValue -> String.format(Locale.getDefault(), format, sliderValue), setter::set);
    }

    private void setupInt(Slider slider, TextView label, int value, IntFunction<String> formatter, IntConsumer setter) {
        setupSlider(slider, label, value, sliderValue -> formatter.apply(sliderValue.intValue()), sliderValue -> setter.accept(sliderValue.intValue()));
    }

    private void setupMs(Slider slider, TextView label, long valueMs, LongConsumer setter) {
        setupSlider(slider, label, valueMs, sliderValue -> String.format(Locale.getDefault(), "%.1fs", sliderValue / 1000f), sliderValue -> setter.accept(sliderValue.longValue()));
    }

    private void setupSlider(Slider slider, TextView label, float initial, Function<Float, String> formatter, Consumer<Float> setter) {
        float clamped = SliderUtil.snap(slider, initial);
        slider.clearOnChangeListeners();
        slider.setLabelFormatter(formatter::apply);
        SliderUtil.setValue(slider, clamped);
        label.setText(formatter.apply(clamped));
        slider.addOnChangeListener((source, value, fromUser) -> {
            if (!fromUser) return;
            float snapped = SliderUtil.snap(source, value);
            setter.accept(snapped);
            label.setText(formatter.apply(snapped));
            applyConfig();
        });
    }

    private void setupSwitch(CompoundButton button, boolean value, Consumer<Boolean> setter, Runnable after) {
        button.setOnCheckedChangeListener(null);
        button.setChecked(value);
        button.setOnCheckedChangeListener((source, checked) -> {
            setter.accept(checked);
            if (after != null) after.run();
            applyConfig();
        });
    }

    private void setupSwitch(CompoundButton button, boolean value, Consumer<Boolean> setter) {
        setupSwitch(button, value, setter, null);
    }

    private void setupChip(ChipGroup group, int initialMode, IntUnaryOperator chipForMode, IntUnaryOperator modeForChip, IntConsumer onChange) {
        group.setOnCheckedStateChangeListener(null);
        group.check(chipForMode.applyAsInt(initialMode));
        group.setOnCheckedStateChangeListener((source, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            onChange.accept(modeForChip.applyAsInt(checkedIds.get(0)));
            applyConfig();
        });
    }

    private String linesText(int value) {
        return value == 0 ? binding.getRoot().getContext().getString(R.string.danmaku_auto) : String.valueOf(value);
    }

    private int visibleIf(boolean condition) {
        return condition ? View.VISIBLE : View.GONE;
    }

    @FunctionalInterface
    private interface FloatSetter {
        void set(float value);
    }
}
