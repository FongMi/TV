package com.fongmi.android.tv.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.DefaultTrackNameProvider;
import androidx.media3.ui.SubtitleView;
import androidx.media3.ui.TrackNameProvider;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogSubtitleSettingBinding;
import com.fongmi.android.tv.databinding.ViewSettingSliderBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.setting.PlayerSetting;
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

    private final DialogSubtitleSettingBinding binding;
    private final SubtitleView subtitleView;
    private final PlayerManager player;
    private boolean refreshAfterSystemSetting;
    private int currentTab;

    SubtitleSettingPanel(DialogSubtitleSettingBinding binding, SubtitleView subtitleView, PlayerManager player) {
        this.binding = binding;
        this.subtitleView = subtitleView;
        this.player = player;
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

    void release() {
    }

    private void bindAppearance() {
        var appearance = binding.appearance;
        bindSystemSetting();
        bindStyleSource();
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

    private void bindAdjust() {
        var adjust = binding.adjust;
        setupSlider(adjust.size, R.string.subtitle_size, SubtitleSetting.MIN_SCALE, SubtitleSetting.MAX_SCALE, STEP_TEXT_SCALE, SubtitleSetting.getScale(), this::formatSize, SubtitleSetting::putScale);
        setupSlider(adjust.position, R.string.subtitle_position, SubtitleSetting.MIN_POSITION, SubtitleSetting.MAX_POSITION, STEP_POSITION_PERCENT, SubtitleSetting.getPosition(), this::formatPosition, SubtitleSetting::putPosition);
    }

    private void bindOffset() {
        setupSlider(binding.offset.timeOffset, R.string.subtitle_offset, MIN_SUBTITLE_OFFSET_MS, MAX_SUBTITLE_OFFSET_MS, STEP_SUBTITLE_OFFSET_MS, getTextOffsetMs(), this::formatOffset, this::setTextOffsetMs);
    }

    private void bindAdvanced() {
        var advanced = binding.advanced;
        SecondaryState state = getSecondaryState();
        bindSecondaryMode(state);
        bindSecondaryTracks(state);
        setupSlider(advanced.secondaryPosition, R.string.subtitle_secondary_position, SubtitleSetting.MIN_SECONDARY_POSITION, SubtitleSetting.MAX_SECONDARY_POSITION, STEP_SECONDARY_POSITION, SubtitleSetting.getSecondaryPosition(), this::formatSecondaryPosition, SubtitleSetting::putSecondaryPosition);
        updateSecondaryControls(state);
    }

    private void bindSecondaryMode(SecondaryState state) {
        ChipGroup group = binding.advanced.secondaryGroup;
        group.setOnCheckedStateChangeListener(null);
        group.check(chipForSecondaryMode(state.trackId()));
        group.setOnCheckedStateChangeListener((source, checkedIds) -> {
            if (checkedIds.isEmpty()) bindSecondaryMode(state);
            else setSecondaryMode(state, secondaryModeForChip(checkedIds.get(0), state.tracks()));
        });
    }

    private void setSecondaryMode(SecondaryState state, int trackId) {
        SubtitleSetting.putSecondaryTrackId(trackId);
        SecondaryState next = state.withTrackId(trackId);
        bindSecondaryTracks(next);
        updateSecondaryControls(next);
        applySubtitleStyle();
    }

    private void bindSecondaryTracks(SecondaryState state) {
        ChipGroup group = binding.advanced.secondaryTrackGroup;
        group.setOnCheckedStateChangeListener(null);
        group.removeAllViews();
        for (SecondaryTrack track : state.tracks()) group.addView(createSecondaryTrackChip(track));
        int chip = chipForSecondaryTrack(state.trackId());
        if (chip != View.NO_ID) group.check(chip);
        group.setOnCheckedStateChangeListener((source, checkedIds) -> {
            if (checkedIds.isEmpty()) bindSecondaryTracks(state);
            else setSecondaryTrack(state, secondaryTrackForChip(checkedIds.get(0)));
        });
    }

    private void setSecondaryTrack(SecondaryState state, int trackId) {
        SubtitleSetting.putSecondaryTrackId(trackId);
        updateSecondaryControls(state.withTrackId(trackId));
        applySubtitleStyle();
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

    private void bindStyleSource() {
        ChipGroup group = binding.appearance.styleSourceGroup;
        group.setOnCheckedStateChangeListener(null);
        group.check(chipForStyleSource(SubtitleSetting.getStyleSource()));
        group.setOnCheckedStateChangeListener((source, checkedIds) -> {
            if (checkedIds.isEmpty()) bindStyleSource();
            else setStyleSource(styleSourceForChip(checkedIds.get(0)));
        });
    }

    private void setStyleSource(int styleSource) {
        SubtitleSetting.putStyleSource(styleSource);
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
        bindAdvanced();
        applySubtitleStyle();
    }

    private void resetAll() {
        SubtitleSetting.reset();
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
            applySubtitleStyle();
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

    private SecondaryState getSecondaryState() {
        boolean supported = isMpvEngine();
        List<SecondaryTrack> tracks = supported ? getSecondaryTracks() : List.of();
        int trackId = supported ? getAvailableSecondarySubtitleTrackId(tracks) : SubtitleSetting.SECONDARY_SUBTITLE_OFF;
        if (supported && trackId != SubtitleSetting.getSecondaryTrackId()) SubtitleSetting.putSecondaryTrackId(trackId);
        return new SecondaryState(supported, trackId, tracks);
    }

    private List<SecondaryTrack> getSecondaryTracks() {
        int primaryTrackId = getPrimarySubtitleTrackId();
        List<SecondaryTrack> tracks = new ArrayList<>();
        TrackNameProvider provider = new DefaultTrackNameProvider(binding.getRoot().getResources());
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != C.TRACK_TYPE_TEXT) continue;
            for (int i = 0; i < group.length; i++) {
                Format format = group.getTrackFormat(i);
                int id = parseTrackId(format);
                if (id >= 0 && id != primaryTrackId) tracks.add(new SecondaryTrack(id, getTrackName(provider, format, id)));
            }
        }
        return tracks;
    }

    private Chip createSecondaryTrackChip(SecondaryTrack track) {
        Context context = new ContextThemeWrapper(binding.getRoot().getContext(), com.google.android.material.R.style.Widget_Material3_Chip_Filter);
        Chip chip = new Chip(context);
        chip.setId(View.generateViewId());
        chip.setTag(track);
        chip.setText(track.name());
        chip.setCheckable(true);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        return chip;
    }

    private String getTrackName(TrackNameProvider provider, Format format, int id) {
        String name = provider.getTrackName(format);
        return TextUtils.isEmpty(name) ? String.valueOf(id) : name;
    }

    private int getAvailableSecondarySubtitleTrackId(List<SecondaryTrack> tracks) {
        int trackId = SubtitleSetting.getSecondaryTrackId();
        if (trackId < 0) return trackId;
        for (SecondaryTrack track : tracks) if (track.id() == trackId) return trackId;
        return SubtitleSetting.SECONDARY_SUBTITLE_AUTO;
    }

    private int chipForSecondaryMode(int trackId) {
        var advanced = binding.advanced;
        int chip = advanced.secondarySelect.getId();
        if (trackId == SubtitleSetting.SECONDARY_SUBTITLE_OFF) chip = advanced.secondaryOff.getId();
        else if (trackId == SubtitleSetting.SECONDARY_SUBTITLE_AUTO) chip = advanced.secondaryAuto.getId();
        return chip;
    }

    private int secondaryModeForChip(int chipId, List<SecondaryTrack> tracks) {
        var advanced = binding.advanced;
        int trackId = SubtitleSetting.SECONDARY_SUBTITLE_AUTO;
        if (chipId == advanced.secondaryOff.getId()) trackId = SubtitleSetting.SECONDARY_SUBTITLE_OFF;
        else if (chipId == advanced.secondarySelect.getId() && !tracks.isEmpty()) trackId = getFirstSecondaryTrackId(tracks);
        return trackId;
    }

    private int chipForSecondaryTrack(int trackId) {
        var advanced = binding.advanced;
        int chipId = View.NO_ID;
        for (int i = 0; i < advanced.secondaryTrackGroup.getChildCount(); i++) {
            View child = advanced.secondaryTrackGroup.getChildAt(i);
            if (child.getTag() instanceof SecondaryTrack track && track.id() == trackId) chipId = child.getId();
        }
        return chipId;
    }

    private int secondaryTrackForChip(int chipId) {
        View chip = binding.advanced.secondaryTrackGroup.findViewById(chipId);
        Object tag = chip == null ? null : chip.getTag();
        int trackId = SubtitleSetting.SECONDARY_SUBTITLE_AUTO;
        if (tag instanceof SecondaryTrack track) trackId = track.id();
        return trackId;
    }

    private int getFirstSecondaryTrackId(List<SecondaryTrack> tracks) {
        return tracks.isEmpty() ? SubtitleSetting.SECONDARY_SUBTITLE_AUTO : tracks.get(0).id();
    }

    private int parseTrackId(Format format) {
        try {
            return format.id == null ? C.INDEX_UNSET : Integer.parseInt(format.id);
        } catch (NumberFormatException e) {
            return C.INDEX_UNSET;
        }
    }

    private int getPrimarySubtitleTrackId() {
        int id = getPrimarySubtitleTrackIdFromOverride();
        return id >= 0 ? id : getPrimarySubtitleTrackIdFromSelection();
    }

    private int getPrimarySubtitleTrackIdFromOverride() {
        if (player == null || player.isReleased()) return C.INDEX_UNSET;
        for (TrackSelectionOverride override : player.getPlayer().getTrackSelectionParameters().overrides.values()) {
            if (override.getType() != C.TRACK_TYPE_TEXT || override.trackIndices.isEmpty()) continue;
            int trackId = parseTrackId(override.mediaTrackGroup.getFormat(override.trackIndices.get(0)));
            if (trackId >= 0) return trackId;
        }
        return C.INDEX_UNSET;
    }

    private int getPrimarySubtitleTrackIdFromSelection() {
        int secondaryTrackId = SubtitleSetting.getSecondaryTrackId();
        int firstSelectedTrackId = C.INDEX_UNSET;
        int primaryTrackId = C.INDEX_UNSET;
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != C.TRACK_TYPE_TEXT || primaryTrackId != C.INDEX_UNSET) continue;
            for (int i = 0; i < group.length; i++) {
                if (!group.isTrackSelected(i)) continue;
                int id = parseTrackId(group.getTrackFormat(i));
                if (id < 0) continue;
                if (firstSelectedTrackId == C.INDEX_UNSET) firstSelectedTrackId = id;
                if (id != secondaryTrackId && primaryTrackId == C.INDEX_UNSET) primaryTrackId = id;
            }
        }
        return primaryTrackId != C.INDEX_UNSET ? primaryTrackId : firstSelectedTrackId;
    }

    private void updateStyleEnabled() {
        boolean textStyle = canApplyTextStyle();
        boolean custom = textStyle && SubtitleSetting.isCustomStyle();
        updateSystemSettingVisibility();
        applyEnabled(binding.appearance.styleSourceHeader, textStyle);
        applyEnabled(binding.appearance.styleSourceGroup, textStyle);
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

    private void updateSecondaryControls(SecondaryState state) {
        var advanced = binding.advanced;
        View section = advanced.secondarySection;
        boolean available = state.supported() && state.hasTracks();
        binding.tabAdvanced.setVisibility(available ? View.VISIBLE : View.GONE);
        if (!available && currentTab == 3) showTab(0);
        section.setVisibility(available ? View.VISIBLE : View.GONE);
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
        if (view instanceof android.view.ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) setEnabledRecursive(group.getChildAt(i), enabled);
    }

    private void applySubtitleStyle() {
        Context context = binding.getRoot().getContext();
        SubtitleSetting.applyStyle(context, subtitleView);
        if (player != null && !player.isReleased()) player.setSubtitleSettingStyle();
    }

    private boolean isMpvEngine() {
        return player != null && !player.isReleased() && player.getEngine() == PlayerSetting.ENGINE_MPV;
    }

    private boolean canApplyTextStyle() {
        Format format = getPrimarySubtitleFormat();
        return format == null || !isImageSubtitle(format.sampleMimeType);
    }

    private boolean isImageSubtitle(String mimeType) {
        return MimeTypes.APPLICATION_PGS.equals(mimeType) || MimeTypes.APPLICATION_VOBSUB.equals(mimeType) || MimeTypes.APPLICATION_DVBSUBS.equals(mimeType);
    }

    private Format getPrimarySubtitleFormat() {
        if (player == null || player.isReleased()) return null;
        Format format = getPrimarySubtitleFormatFromOverride();
        return format != null ? format : getPrimarySubtitleFormatFromSelection();
    }

    private Format getPrimarySubtitleFormatFromOverride() {
        for (TrackSelectionOverride override : player.getPlayer().getTrackSelectionParameters().overrides.values()) {
            if (override.getType() != C.TRACK_TYPE_TEXT || override.trackIndices.isEmpty()) continue;
            return override.mediaTrackGroup.getFormat(override.trackIndices.get(0));
        }
        return null;
    }

    private Format getPrimarySubtitleFormatFromSelection() {
        int secondaryTrackId = SubtitleSetting.getSecondaryTrackId();
        Format firstSelectedFormat = null;
        Format primaryFormat = null;
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != C.TRACK_TYPE_TEXT || primaryFormat != null) continue;
            for (int i = 0; i < group.length; i++) {
                if (!group.isTrackSelected(i)) continue;
                Format format = group.getTrackFormat(i);
                int id = parseTrackId(format);
                if (firstSelectedFormat == null) firstSelectedFormat = format;
                if (id != secondaryTrackId && primaryFormat == null) primaryFormat = format;
            }
        }
        return primaryFormat != null ? primaryFormat : firstSelectedFormat;
    }

    private int chipForStyleSource(int source) {
        var appearance = binding.appearance;
        int chip = appearance.styleOriginal.getId();
        if (source == SubtitleSetting.STYLE_SOURCE_SYSTEM) chip = appearance.styleSystem.getId();
        else if (source == SubtitleSetting.STYLE_SOURCE_CUSTOM) chip = appearance.styleCustom.getId();
        return chip;
    }

    private int styleSourceForChip(int chipId) {
        var appearance = binding.appearance;
        int source = SubtitleSetting.STYLE_SOURCE_ORIGINAL;
        if (chipId == appearance.styleSystem.getId()) source = SubtitleSetting.STYLE_SOURCE_SYSTEM;
        else if (chipId == appearance.styleCustom.getId()) source = SubtitleSetting.STYLE_SOURCE_CUSTOM;
        return source;
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
        return player == null || player.isReleased() ? 0.0f : player.getTextOffsetMs();
    }

    private void setTextOffsetMs(float offsetMs) {
        if (player != null && !player.isReleased()) player.setTextOffsetMs(Math.round(offsetMs));
    }

    private record SecondaryState(boolean supported, int trackId, List<SecondaryTrack> tracks) {

        private SecondaryState withTrackId(int trackId) {
            return new SecondaryState(supported, trackId, tracks);
        }

        private boolean hasTracks() {
            return !tracks.isEmpty();
        }

        private boolean usesSpecificTrack() {
            return trackId >= 0;
        }

        private boolean isEnabled() {
            return supported && hasTracks() && trackId != SubtitleSetting.SECONDARY_SUBTITLE_OFF;
        }
    }

    private record SecondaryTrack(int id, String name) {
    }

    private interface ValueFormatter {
        String format(float value);
    }
}
