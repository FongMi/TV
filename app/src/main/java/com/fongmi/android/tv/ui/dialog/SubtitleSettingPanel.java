package com.fongmi.android.tv.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.DefaultTrackNameProvider;
import androidx.media3.ui.SubtitleView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogSubtitleSettingBinding;
import com.fongmi.android.tv.databinding.ViewSettingSliderBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.player.engine.PlayerEngine.SecondarySubtitleState;
import com.fongmi.android.tv.player.subtitle.ExternalFont;
import com.fongmi.android.tv.setting.SubtitleSetting;
import com.fongmi.android.tv.utils.SliderUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;

final class SubtitleSettingPanel {

    private static final float MIN_SUBTITLE_OFFSET_MS = -300000.0f;
    private static final float MAX_SUBTITLE_OFFSET_MS = 300000.0f;
    private static final float STEP_SUBTITLE_OFFSET_MS = 1000.0f;
    private static final float STEP_TEXT_SCALE = 0.05f;
    private static final float STEP_POSITION_PERCENT = 0.5f;
    private static final float STEP_OPACITY = 0.05f;
    private static final float STEP_EDGE = 0.5f;
    private static final float STEP_SECONDARY_POSITION = 1.0f;
    private static final int SECONDARY_UI_MODE_SELECT = 0;

    private final DialogSubtitleSettingBinding binding;
    private final SubtitleView subtitleView;
    private final PlayerManager player;
    private final ExternalFontSelector fontSelector;

    private boolean refreshAfterSystemSetting;
    private int currentTab;

    SubtitleSettingPanel(DialogSubtitleSettingBinding binding, SubtitleView subtitleView, PlayerManager player, ExternalFontSelector fontSelector) {
        this.binding = binding;
        this.subtitleView = subtitleView;
        this.player = player;
        this.fontSelector = fontSelector;
    }

    void bind() {
        bindAppearance();
        bindAdjust();
        bindOffset();
        bindAdvanced();
        bindTabs();
        bindReset();
        showTab(0);
        if (Util.isLeanback()) binding.tabAppearance.requestFocus();
        binding.tabGroup.check(binding.tabAppearance.getId());
    }

    void onResume() {
        if (!refreshAfterSystemSetting) return;
        refreshAfterSystemSetting = false;
        applySubtitleStyle();
    }

    void onFontSelected(@Nullable ExternalFont.Item font) {
        SubtitleSetting.putFont(font);
        applySubtitleStyle();
    }

    private void bindAppearance() {
        var appearance = binding.appearance;
        bindSystemSetting();
        bindStyle();
        bindFont();
        setupChip(appearance.textColorGroup, SubtitleSetting.getTextBaseColor(), this::chipForTextColor, this::textColorForChip, SubtitleSetting::putTextColor);
        setupTransparency(appearance.textOpacity, R.string.subtitle_text_opacity, SubtitleSetting.getTextOpacity(), SubtitleSetting::putTextOpacity);
        setupChip(appearance.edgeGroup, SubtitleSetting.getEdgeType(), this::chipForEdgeType, this::edgeTypeForChip, value -> {
            SubtitleSetting.putEdgeType(value);
            updateEdgeControls();
        });
        setupChip(appearance.edgeColorGroup, SubtitleSetting.getEdgeBaseColor(), this::chipForEdgeColor, this::edgeColorForChip, SubtitleSetting::putEdgeColor);
        setupTransparency(appearance.edgeOpacity, R.string.subtitle_edge_opacity, SubtitleSetting.getEdgeOpacity(), SubtitleSetting::putEdgeOpacity);
        setupSlider(appearance.edgeWidth, R.string.subtitle_edge_width, SubtitleSetting.MIN_EDGE_WIDTH, SubtitleSetting.MAX_EDGE_WIDTH, STEP_EDGE, SubtitleSetting.getEdgeWidth(), this::formatDecimal, SubtitleSetting::putEdgeWidth);
        setupSlider(appearance.shadow, R.string.subtitle_shadow_strength, SubtitleSetting.MIN_SHADOW, SubtitleSetting.MAX_SHADOW, STEP_EDGE, SubtitleSetting.getShadow(), this::formatDecimal, SubtitleSetting::putShadow);
        setupChip(appearance.backgroundGroup, SubtitleSetting.getBackgroundBaseColor(), this::chipForBackgroundColor, this::backgroundColorForChip, value -> {
            SubtitleSetting.putBackgroundColor(value);
            updateBackgroundControls();
        });
        setupTransparency(appearance.backgroundOpacity, R.string.subtitle_background_opacity, SubtitleSetting.getBackgroundOpacity(), SubtitleSetting::putBackgroundOpacity);
        updateStyleEnabled();
    }

    private void bindFont() {
        fontSelector.bind(binding.appearance.fontGroup, SubtitleSetting.getFont());
    }

    private void bindAdjust() {
        var adjust = binding.adjust;
        setupSlider(adjust.size, R.string.subtitle_size, SubtitleSetting.MIN_SCALE, SubtitleSetting.MAX_SCALE, STEP_TEXT_SCALE, SubtitleSetting.getScale(), this::formatSize, SubtitleSetting::putScale);
        setupSlider(adjust.position, R.string.subtitle_position, SubtitleSetting.MIN_POSITION, SubtitleSetting.MAX_POSITION, STEP_POSITION_PERCENT, SubtitleSetting.getPosition(), this::formatPosition, SubtitleSetting::putPosition);
    }

    private void bindOffset() {
        setupSlider(binding.offset.timeOffset, R.string.subtitle_offset, MIN_SUBTITLE_OFFSET_MS, MAX_SUBTITLE_OFFSET_MS, STEP_SUBTITLE_OFFSET_MS, getTextOffsetMs(), this::formatOffset, this::setTextOffsetMs, false);
    }

    private void bindAdvanced() {
        var advanced = binding.advanced;
        SecondarySubtitleUiState state = getSecondarySubtitleUiState();
        bindSecondaryMode(state);
        bindSecondaryTracks(state);
        setupSlider(advanced.secondaryPosition, R.string.subtitle_secondary_position, SubtitleSetting.MIN_SECONDARY_POSITION, SubtitleSetting.MAX_SECONDARY_POSITION, STEP_SECONDARY_POSITION, SubtitleSetting.getSecondaryPosition(), this::formatSecondaryPosition, SubtitleSetting::putSecondaryPosition);
        updateSecondaryControls(state);
    }

    private void bindSecondaryMode(SecondarySubtitleUiState state) {
        ChipGroup group = binding.advanced.secondaryGroup;
        group.setOnCheckedStateChangeListener(null);
        group.check(chipForSecondaryMode(state.mode()));
        group.setOnCheckedStateChangeListener((source, checkedIds) -> {
            if (checkedIds.isEmpty()) bindSecondaryMode(state);
            else applySecondaryMode(state, secondaryModeForChip(checkedIds.get(0), state.options()));
        });
    }

    private void applySecondaryMode(SecondarySubtitleUiState state, int mode) {
        boolean selectTrack = mode == SECONDARY_UI_MODE_SELECT;
        SecondaryTrackOption selectedOption = selectTrack ? getFirstSecondaryTrackOption(state.options()) : null;
        SubtitleSetting.putSecondaryMode(selectTrack ? SubtitleSetting.SECONDARY_MODE_AUTO : mode);
        SecondarySubtitleUiState next = state.withSelection(mode, selectedOption);
        setSecondarySubtitleSelection(selectedOption == null ? null : selectedOption.selection());
        bindSecondaryTracks(next);
        updateSecondaryControls(next);
    }

    private void bindSecondaryTracks(SecondarySubtitleUiState state) {
        ChipGroup group = binding.advanced.secondaryTrackGroup;
        group.setOnCheckedStateChangeListener(null);
        group.removeAllViews();
        for (SecondaryTrackOption option : state.options()) group.addView(createSecondaryTrackChip(option));
        int chip = chipForSecondaryTrack(state.selectedOption());
        if (chip != View.NO_ID) group.check(chip);
        group.setOnCheckedStateChangeListener((source, checkedIds) -> {
            if (checkedIds.isEmpty()) bindSecondaryTracks(state);
            else selectSecondaryTrack(state, secondaryTrackForChip(checkedIds.get(0)));
        });
    }

    private void selectSecondaryTrack(SecondarySubtitleUiState state, @Nullable SecondaryTrackOption option) {
        if (option != null) {
            SubtitleSetting.putSecondaryMode(SubtitleSetting.SECONDARY_MODE_AUTO);
            setSecondarySubtitleSelection(option.selection());
            updateSecondaryControls(state.withSelection(SECONDARY_UI_MODE_SELECT, option));
        } else {
            bindSecondaryTracks(state);
        }
    }

    private void bindSystemSetting() {
        binding.appearance.systemSetting.setOnClickListener(this::openSystemCaptionSettings);
        updateSystemSettingVisibility();
    }

    private boolean hasSystemCaptionSettings() {
        Context context = binding.getRoot().getContext();
        return new Intent(Settings.ACTION_CAPTIONING_SETTINGS).resolveActivity(context.getPackageManager()) != null;
    }

    private void openSystemCaptionSettings(View view) {
        refreshAfterSystemSetting = true;
        view.getContext().startActivity(new Intent(Settings.ACTION_CAPTIONING_SETTINGS));
    }

    private void bindStyle() {
        ChipGroup group = binding.appearance.styleGroup;
        group.setOnCheckedStateChangeListener(null);
        group.check(chipForStyle(SubtitleSetting.getStyleMode()));
        group.setOnCheckedStateChangeListener((source, checkedIds) -> {
            if (checkedIds.isEmpty()) bindStyle();
            else setStyle(styleForChip(checkedIds.get(0)));
        });
    }

    private void setStyle(int style) {
        SubtitleSetting.putStyleMode(style);
        updateStyleEnabled();
        applySubtitleStyle();
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

    private void bindReset() {
        binding.reset.setOnClickListener(this::onReset);
        binding.reset.setOnLongClickListener(view -> {
            resetAll();
            return true;
        });
    }

    private void onReset(View view) {
        switch (currentTab) {
            case 0 -> resetAppearance();
            case 1 -> resetAdjust();
            case 2 -> resetOffset();
            case 3 -> resetAdvanced();
        }
    }

    private void resetAppearance() {
        SubtitleSetting.resetStyle();
        bindAppearance();
        applySubtitleStyle();
    }

    private void resetAdjust() {
        SubtitleSetting.resetAdjust();
        bindAdjust();
        applySubtitleStyle();
    }

    private void resetOffset() {
        setTextOffsetMs(0.0f);
        bindOffset();
    }

    private void resetAdvanced() {
        SubtitleSetting.resetAdvanced();
        setSecondarySubtitleSelection(null);
        bindAdvanced();
        applySubtitleStyle();
    }

    private void resetAll() {
        SubtitleSetting.reset();
        setSecondarySubtitleSelection(null);
        setTextOffsetMs(0.0f);
        bindAppearance();
        bindAdjust();
        bindOffset();
        bindAdvanced();
        applySubtitleStyle();
    }

    private void showTab(int index) {
        View[] roots = {binding.appearance.getRoot(), binding.adjust.getRoot(), binding.offset.getRoot(), binding.advanced.getRoot()};
        MaterialButton[] tabs = getTabs();
        for (int i = 0; i < roots.length; i++) roots[i].setVisibility(index == i ? View.VISIBLE : View.GONE);
        binding.reset.setNextFocusDownId(tabs[currentTab = index].getId());
    }

    private MaterialButton[] getTabs() {
        return new MaterialButton[]{binding.tabAppearance, binding.tabAdjust, binding.tabOffset, binding.tabAdvanced};
    }

    private void setupSlider(ViewSettingSliderBinding item, int titleRes, float from, float to, float step, float initial, ValueFormatter formatter, Consumer<Float> setter) {
        setupSlider(item, titleRes, from, to, step, initial, formatter, setter, true);
    }

    private void setupSlider(ViewSettingSliderBinding item, int titleRes, float from, float to, float step, float initial, ValueFormatter formatter, Consumer<Float> setter, boolean applyStyle) {
        item.title.setText(titleRes);
        Slider slider = item.slider;
        float clamped = SliderUtil.snap(initial, from, to, step);
        slider.clearOnChangeListeners();
        slider.setValueFrom(from);
        slider.setValueTo(to);
        slider.setStepSize(step);
        slider.setLabelFormatter(formatter::format);
        SliderUtil.setValue(slider, clamped);
        item.value.setText(formatter.format(clamped));
        slider.addOnChangeListener((source, value, fromUser) -> {
            if (!fromUser) return;
            float snapped = SliderUtil.snap(source, value);
            setter.accept(snapped);
            item.value.setText(formatter.format(snapped));
            if (applyStyle) applySubtitleStyle();
        });
    }

    private void setupTransparency(ViewSettingSliderBinding item, int titleRes, float opacity, Consumer<Float> setter) {
        setupSlider(item, titleRes, SubtitleSetting.MIN_OPACITY, SubtitleSetting.MAX_OPACITY, STEP_OPACITY, toTransparency(opacity), this::formatPercent, value -> setter.accept(toOpacity(value)));
    }

    private void setupChip(ChipGroup group, int initialValue, IntUnaryOperator chipForValue, IntUnaryOperator valueForChip, IntConsumer setter) {
        group.setOnCheckedStateChangeListener(null);
        group.clearCheck();
        int chip = chipForValue.applyAsInt(initialValue);
        if (chip != View.NO_ID) group.check(chip);
        group.setOnCheckedStateChangeListener((source, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            setter.accept(valueForChip.applyAsInt(checkedIds.get(0)));
            applySubtitleStyle();
        });
    }

    private SecondarySubtitleUiState getSecondarySubtitleUiState() {
        SecondarySubtitleState state = getSecondarySubtitleState();
        List<SecondaryTrackOption> options = buildSecondaryTrackOptions(state.secondaryCandidates());
        SecondaryTrackOption selectedOption = findSecondaryTrackOption(options, state.explicitSelection());
        int mode = state.secondaryPromotedToPrimary() ? SubtitleSetting.SECONDARY_MODE_OFF : SubtitleSetting.getSecondaryMode();
        if (selectedOption != null) mode = SECONDARY_UI_MODE_SELECT;
        return new SecondarySubtitleUiState(mode, selectedOption, options);
    }

    private List<SecondaryTrackOption> buildSecondaryTrackOptions(List<TrackSelectionOverride> candidates) {
        List<SecondaryTrackOption> options = new ArrayList<>(candidates.size());
        DefaultTrackNameProvider provider = new DefaultTrackNameProvider(binding.getRoot().getResources());
        for (TrackSelectionOverride selection : candidates) options.add(new SecondaryTrackOption(selection, provider.getTrackName(getFormat(selection))));
        return options;
    }

    private Chip createSecondaryTrackChip(SecondaryTrackOption option) {
        Chip chip = (Chip) LayoutInflater.from(binding.getRoot().getContext()).inflate(R.layout.view_filter_chip, binding.advanced.secondaryTrackGroup, false);
        chip.setId(View.generateViewId());
        chip.setTag(option);
        chip.setText(option.name());
        chip.setCheckable(true);
        return chip;
    }

    @Nullable
    private SecondaryTrackOption findSecondaryTrackOption(List<SecondaryTrackOption> options, @Nullable TrackSelectionOverride selection) {
        if (selection == null) return null;
        for (SecondaryTrackOption option : options) if (option.selection().equals(selection)) return option;
        return null;
    }

    private int chipForSecondaryMode(int mode) {
        var advanced = binding.advanced;
        int chip = advanced.secondaryDefault.getId();
        if (mode == SubtitleSetting.SECONDARY_MODE_OFF) chip = advanced.secondaryOff.getId();
        else if (mode == SubtitleSetting.SECONDARY_MODE_AUTO) chip = advanced.secondaryAuto.getId();
        else if (mode == SECONDARY_UI_MODE_SELECT) chip = advanced.secondarySelect.getId();
        return chip;
    }

    private int secondaryModeForChip(int chipId, List<SecondaryTrackOption> options) {
        var advanced = binding.advanced;
        int mode = SubtitleSetting.SECONDARY_MODE_DEFAULT;
        if (chipId == advanced.secondaryOff.getId()) mode = SubtitleSetting.SECONDARY_MODE_OFF;
        else if (chipId == advanced.secondaryAuto.getId()) mode = SubtitleSetting.SECONDARY_MODE_AUTO;
        else if (chipId == advanced.secondarySelect.getId() && !options.isEmpty()) mode = SECONDARY_UI_MODE_SELECT;
        return mode;
    }

    private int chipForSecondaryTrack(@Nullable SecondaryTrackOption selectedOption) {
        var advanced = binding.advanced;
        int chipId = View.NO_ID;
        for (int i = 0; i < advanced.secondaryTrackGroup.getChildCount(); i++) {
            View child = advanced.secondaryTrackGroup.getChildAt(i);
            if (child.getTag() instanceof SecondaryTrackOption option && option.equals(selectedOption)) chipId = child.getId();
        }
        return chipId;
    }

    @Nullable
    private SecondaryTrackOption secondaryTrackForChip(int chipId) {
        View chip = binding.advanced.secondaryTrackGroup.findViewById(chipId);
        Object tag = chip == null ? null : chip.getTag();
        return tag instanceof SecondaryTrackOption option ? option : null;
    }

    @Nullable
    private SecondaryTrackOption getFirstSecondaryTrackOption(List<SecondaryTrackOption> options) {
        return options.isEmpty() ? null : options.get(0);
    }

    private void updateStyleEnabled() {
        boolean textStyle = canApplyTextStyle();
        boolean custom = textStyle && SubtitleSetting.isCustomStyle();
        updateSystemSettingVisibility();
        applyEnabled(binding.appearance.styleHeader, textStyle);
        applyEnabled(binding.appearance.styleGroup, textStyle);
        binding.appearance.fontHeader.setVisibility(textStyle ? View.VISIBLE : View.GONE);
        binding.appearance.fontContainer.setVisibility(textStyle ? View.VISIBLE : View.GONE);
        binding.appearance.textSection.setVisibility(custom ? View.VISIBLE : View.GONE);
        binding.appearance.edgeStyleSection.setVisibility(custom ? View.VISIBLE : View.GONE);
        binding.appearance.backgroundSection.setVisibility(custom ? View.VISIBLE : View.GONE);
        updateEdgeControls();
        updateBackgroundControls();
    }

    private void updateEdgeControls() {
        var appearance = binding.appearance;
        boolean custom = canApplyTextStyle() && SubtitleSetting.isCustomStyle();
        int edgeType = SubtitleSetting.getEdgeType();
        boolean hasEdge = edgeType != CaptionStyleCompat.EDGE_TYPE_NONE;
        appearance.edgeColorSection.setVisibility(custom && hasEdge ? View.VISIBLE : View.GONE);
        appearance.edgeWidth.getRoot().setVisibility(custom && edgeType == CaptionStyleCompat.EDGE_TYPE_OUTLINE ? View.VISIBLE : View.GONE);
        appearance.shadow.getRoot().setVisibility(custom && edgeType == CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW ? View.VISIBLE : View.GONE);
    }

    private void updateBackgroundControls() {
        boolean custom = canApplyTextStyle() && SubtitleSetting.isCustomStyle();
        int color = SubtitleSetting.getBackgroundBaseColor();
        binding.appearance.backgroundOpacity.getRoot().setVisibility(custom && Color.alpha(color) > 0 ? View.VISIBLE : View.GONE);
    }

    private void updateSecondaryControls(SecondarySubtitleUiState state) {
        var advanced = binding.advanced;
        boolean available = state.hasTracks();
        binding.tabAdvanced.setVisibility(available ? View.VISIBLE : View.GONE);
        if (!available && currentTab == 3) showTab(0);
        advanced.secondarySection.setVisibility(available ? View.VISIBLE : View.GONE);
        advanced.secondarySelect.setVisibility(available ? View.VISIBLE : View.GONE);
        advanced.secondaryTrackSection.setVisibility(available && state.usesSpecificTrack() ? View.VISIBLE : View.GONE);
        advanced.secondaryPosition.getRoot().setVisibility(state.isEnabled() ? View.VISIBLE : View.GONE);
    }

    private void updateSystemSettingVisibility() {
        boolean visible = canApplyTextStyle() && hasSystemCaptionSettings() && SubtitleSetting.isSystemStyle();
        binding.appearance.systemSetting.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void applyEnabled(View view, boolean enabled) {
        view.setAlpha(enabled ? 1.0f : 0.38f);
        setEnabledRecursive(view, enabled);
    }

    private void setEnabledRecursive(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) setEnabledRecursive(group.getChildAt(i), enabled);
    }

    private void applySubtitleStyle() {
        SubtitleSetting.applyStyle(subtitleView);
        if (isPlayerAvailable()) player.applySubtitleStyle();
    }

    private boolean canApplyTextStyle() {
        Format format = getPrimarySubtitleFormat();
        return format == null || !isImageSubtitle(format.sampleMimeType);
    }

    private static boolean isImageSubtitle(@Nullable String mimeType) {
        return MimeTypes.APPLICATION_PGS.equals(mimeType) || MimeTypes.APPLICATION_VOBSUB.equals(mimeType) || MimeTypes.APPLICATION_DVBSUBS.equals(mimeType);
    }

    private Format getPrimarySubtitleFormat() {
        TrackSelectionOverride selection = getSecondarySubtitleState().primarySelection();
        return selection == null ? null : getFormat(selection);
    }

    private static Format getFormat(TrackSelectionOverride selection) {
        return selection.mediaTrackGroup.getFormat(selection.trackIndices.get(0));
    }

    private int chipForStyle(int style) {
        var appearance = binding.appearance;
        int chip = appearance.styleOriginal.getId();
        if (style == SubtitleSetting.STYLE_SYSTEM) chip = appearance.styleSystem.getId();
        else if (style == SubtitleSetting.STYLE_CUSTOM) chip = appearance.styleCustom.getId();
        return chip;
    }

    private int styleForChip(int chipId) {
        var appearance = binding.appearance;
        int style = SubtitleSetting.STYLE_ORIGINAL;
        if (chipId == appearance.styleSystem.getId()) style = SubtitleSetting.STYLE_SYSTEM;
        else if (chipId == appearance.styleCustom.getId()) style = SubtitleSetting.STYLE_CUSTOM;
        return style;
    }

    private int chipForTextColor(int color) {
        var appearance = binding.appearance;
        int chip = appearance.textWhite.getId();
        if (color == SubtitleSetting.SUBTITLE_COLOR_YELLOW) chip = appearance.textYellow.getId();
        else if (color == SubtitleSetting.SUBTITLE_COLOR_CYAN) chip = appearance.textCyan.getId();
        else if (color == SubtitleSetting.SUBTITLE_COLOR_GREEN) chip = appearance.textGreen.getId();
        else if (color == SubtitleSetting.SUBTITLE_COLOR_ORANGE) chip = appearance.textOrange.getId();
        else if (color == SubtitleSetting.SUBTITLE_COLOR_PINK) chip = appearance.textPink.getId();
        else if (color == SubtitleSetting.SUBTITLE_COLOR_RED) chip = appearance.textRed.getId();
        else if (color == SubtitleSetting.SUBTITLE_COLOR_BLUE) chip = appearance.textBlue.getId();
        return chip;
    }

    private int textColorForChip(int chipId) {
        var appearance = binding.appearance;
        int color = SubtitleSetting.SUBTITLE_COLOR_WHITE;
        if (chipId == appearance.textYellow.getId()) color = SubtitleSetting.SUBTITLE_COLOR_YELLOW;
        else if (chipId == appearance.textCyan.getId()) color = SubtitleSetting.SUBTITLE_COLOR_CYAN;
        else if (chipId == appearance.textGreen.getId()) color = SubtitleSetting.SUBTITLE_COLOR_GREEN;
        else if (chipId == appearance.textOrange.getId()) color = SubtitleSetting.SUBTITLE_COLOR_ORANGE;
        else if (chipId == appearance.textPink.getId()) color = SubtitleSetting.SUBTITLE_COLOR_PINK;
        else if (chipId == appearance.textRed.getId()) color = SubtitleSetting.SUBTITLE_COLOR_RED;
        else if (chipId == appearance.textBlue.getId()) color = SubtitleSetting.SUBTITLE_COLOR_BLUE;
        return color;
    }

    private int chipForEdgeType(int edgeType) {
        var appearance = binding.appearance;
        int chip = appearance.edgeOutline.getId();
        if (edgeType == CaptionStyleCompat.EDGE_TYPE_NONE) chip = appearance.edgeNone.getId();
        else if (edgeType == CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW) chip = appearance.edgeShadow.getId();
        return chip;
    }

    private int edgeTypeForChip(int chipId) {
        var appearance = binding.appearance;
        int edgeType = CaptionStyleCompat.EDGE_TYPE_OUTLINE;
        if (chipId == appearance.edgeNone.getId()) edgeType = CaptionStyleCompat.EDGE_TYPE_NONE;
        else if (chipId == appearance.edgeShadow.getId()) edgeType = CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW;
        return edgeType;
    }

    private int chipForEdgeColor(int color) {
        var appearance = binding.appearance;
        int chip = appearance.edgeBlack.getId();
        if (color == Color.WHITE) chip = appearance.edgeWhite.getId();
        else if (color == SubtitleSetting.SUBTITLE_COLOR_GRAY) chip = appearance.edgeGray.getId();
        else if (color == SubtitleSetting.SUBTITLE_COLOR_YELLOW) chip = appearance.edgeYellow.getId();
        return chip;
    }

    private int edgeColorForChip(int chipId) {
        var appearance = binding.appearance;
        int color = SubtitleSetting.SUBTITLE_COLOR_BLACK;
        if (chipId == appearance.edgeWhite.getId()) color = SubtitleSetting.SUBTITLE_COLOR_WHITE;
        else if (chipId == appearance.edgeGray.getId()) color = SubtitleSetting.SUBTITLE_COLOR_GRAY;
        else if (chipId == appearance.edgeYellow.getId()) color = SubtitleSetting.SUBTITLE_COLOR_YELLOW;
        return color;
    }

    private int chipForBackgroundColor(int color) {
        var appearance = binding.appearance;
        int chip = appearance.backgroundTransparent.getId();
        if (color == SubtitleSetting.SUBTITLE_BACKGROUND_DIM) chip = appearance.backgroundDim.getId();
        else if (color == SubtitleSetting.SUBTITLE_BACKGROUND_BLACK) chip = appearance.backgroundBlack.getId();
        else if (color == SubtitleSetting.SUBTITLE_BACKGROUND_GRAY) chip = appearance.backgroundGray.getId();
        return chip;
    }

    private int backgroundColorForChip(int chipId) {
        var appearance = binding.appearance;
        int color = Color.TRANSPARENT;
        if (chipId == appearance.backgroundDim.getId()) color = SubtitleSetting.SUBTITLE_BACKGROUND_DIM;
        else if (chipId == appearance.backgroundBlack.getId()) color = SubtitleSetting.SUBTITLE_BACKGROUND_BLACK;
        else if (chipId == appearance.backgroundGray.getId()) color = SubtitleSetting.SUBTITLE_BACKGROUND_GRAY;
        return color;
    }

    private String formatSize(float value) {
        return String.format(Locale.getDefault(), "%.0f%%", value * 100.0f);
    }

    private String formatPosition(float value) {
        return String.format(Locale.getDefault(), "%+.1f%%", value);
    }

    private String formatOffset(float offsetMs) {
        return String.format(Locale.getDefault(), "%+.1fs", offsetMs / 1000.0f);
    }

    private String formatPercent(float value) {
        return String.format(Locale.getDefault(), "%.0f%%", value * 100.0f);
    }

    private float toTransparency(float opacity) {
        return 1.0f - opacity;
    }

    private float toOpacity(float transparency) {
        return 1.0f - transparency;
    }

    private String formatDecimal(float value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    private String formatSecondaryPosition(float value) {
        return String.format(Locale.getDefault(), "%.0f%%", value);
    }

    private float getTextOffsetMs() {
        return isPlayerAvailable() ? player.getTextOffsetMs() : 0.0f;
    }

    private void setTextOffsetMs(float offsetMs) {
        if (isPlayerAvailable()) player.setTextOffsetMs(Math.round(offsetMs));
    }

    private void setSecondarySubtitleSelection(@Nullable TrackSelectionOverride selection) {
        if (isPlayerAvailable()) player.setSecondarySubtitleSelection(selection);
    }

    private SecondarySubtitleState getSecondarySubtitleState() {
        return isPlayerAvailable() ? player.getSecondarySubtitleState() : SecondarySubtitleState.EMPTY;
    }

    private boolean isPlayerAvailable() {
        return player != null && !player.isReleased();
    }

    private record SecondarySubtitleUiState(int mode, @Nullable SecondaryTrackOption selectedOption, List<SecondaryTrackOption> options) {

        private SecondarySubtitleUiState withSelection(int mode, @Nullable SecondaryTrackOption selectedOption) {
            return new SecondarySubtitleUiState(mode, selectedOption, options);
        }

        private boolean hasTracks() {
            return !options.isEmpty();
        }

        private boolean usesSpecificTrack() {
            return mode == SECONDARY_UI_MODE_SELECT;
        }

        private boolean isEnabled() {
            return hasTracks() && mode != SubtitleSetting.SECONDARY_MODE_OFF;
        }
    }

    private record SecondaryTrackOption(TrackSelectionOverride selection, String name) {
    }

    private interface ValueFormatter {
        String format(float value);
    }
}
