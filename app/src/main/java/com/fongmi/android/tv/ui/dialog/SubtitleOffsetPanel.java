package com.fongmi.android.tv.ui.dialog;

import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.text.SubtitleOffsets;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ViewSettingSliderBinding;
import com.fongmi.android.tv.databinding.ViewSubtitleOffsetBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.SliderUtil;
import com.fongmi.android.tv.utils.SubtitleOffsetUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;
import java.util.Objects;

final class SubtitleOffsetPanel {

    private static final float STEP_MS = 100;

    private final ViewSubtitleOffsetBinding binding;
    private final PlayerManager player;
    private AlertDialog resetDialog;

    SubtitleOffsetPanel(ViewSubtitleOffsetBinding binding, PlayerManager player) {
        this.binding = binding;
        this.player = player;
    }

    void bind() {
        boolean secondary = hasSecondary();
        boolean restoreFocus = !secondary && (binding.secondaryOffset.getRoot().hasFocus() || binding.linkOffsetsRow.hasFocus());
        binding.secondaryOffset.getRoot().setVisibility(secondary ? View.VISIBLE : View.GONE);
        binding.linkOffsetsRow.setVisibility(secondary ? View.VISIBLE : View.GONE);
        if (!secondary) binding.linkOffsets.setChecked(false);
        bindSlider(binding.timeOffset, secondary ? R.string.subtitle_offset_primary : R.string.subtitle_offset, true);
        bindSlider(binding.secondaryOffset, R.string.subtitle_offset_secondary, false);
        updateValues();
        if (restoreFocus) binding.timeOffset.slider.requestFocus();
    }

    private void bindSlider(ViewSettingSliderBinding item, int title, boolean primary) {
        item.title.setText(title);
        item.slider.setContentDescription(item.title.getText());
        item.slider.clearOnChangeListeners();
        item.slider.setValueFrom(-SubtitleOffsetUtil.MAX_OFFSET_MS);
        item.slider.setValueTo(SubtitleOffsetUtil.MAX_OFFSET_MS);
        item.slider.setStepSize(STEP_MS);
        item.slider.setLabelFormatter(this::format);
        item.slider.setEnabled(isAvailable() && (primary || hasSecondary()));
        item.slider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) setOffset(Math.round(value), primary);
        });
    }

    private void setOffset(long valueMs, boolean primary) {
        if (!isAvailable() || !primary && !hasSecondary()) return;
        SubtitleOffsets previous = player.getSubtitleOffsets();
        boolean linked = hasSecondary() && binding.linkOffsets.isChecked();
        long deltaMs = valueMs - (primary ? previous.primaryMs : previous.secondaryMs);
        SubtitleOffsets next = SubtitleOffsetUtil.shift(previous, deltaMs, primary || linked, !primary || linked);
        if (next != null) player.setSubtitleOffsets(next);
        else Notify.show(R.string.subtitle_offset_limit);
        updateValues();
    }

    private void updateValues() {
        SubtitleOffsets offsets = isAvailable() ? player.getSubtitleOffsets() : SubtitleOffsets.ZERO;
        updateValue(binding.timeOffset, offsets.primaryMs);
        updateValue(binding.secondaryOffset, offsets.secondaryMs);
    }

    private void updateValue(ViewSettingSliderBinding item, long valueMs) {
        SliderUtil.setValue(item.slider, valueMs);
        // The thumb uses 100 ms steps, but reading the setting must not round a prior calibration.
        item.value.setText(format(valueMs));
    }

    private String format(float valueMs) {
        return String.format(Locale.getDefault(), "%+.3fs", valueMs / 1000.0f);
    }

    void reset() {
        if (!isAvailable()) return;
        if (!hasSecondary()) {
            reset(true, false);
            return;
        }
        if (resetDialog != null) return;
        Player source = player.getPlayer();
        MediaItem media = player.getCurrentMediaItem();
        var selection = player.getSubtitleSelectionState();
        String[] choices = {
                binding.getRoot().getContext().getString(R.string.subtitle_offset_primary),
                binding.getRoot().getContext().getString(R.string.subtitle_offset_secondary),
                binding.getRoot().getContext().getString(R.string.subtitle_offset_both)
        };
        resetDialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setTitle(R.string.subtitle_offset_reset).setItems(choices, (dialog, which) -> {
            if (isAvailable() && source == player.getPlayer() && Objects.equals(media, player.getCurrentMediaItem()) && selection.equals(player.getSubtitleSelectionState())) {
                reset(which != 1, which != 0);
            } else {
                bind();
            }
        }).setNegativeButton(R.string.dialog_negative, null).create();
        resetDialog.setOnDismissListener(dialog -> resetDialog = null);
        resetDialog.show();
    }

    void resetAll() {
        reset(true, true);
    }

    private void reset(boolean primary, boolean secondary) {
        if (!isAvailable()) return;
        SubtitleOffsets previous = player.getSubtitleOffsets();
        player.setSubtitleOffsets(new SubtitleOffsets(primary ? 0 : previous.primaryMs, secondary ? 0 : previous.secondaryMs));
        updateValues();
    }

    void release() {
        if (resetDialog != null) resetDialog.dismiss();
    }

    private boolean hasSecondary() {
        return isAvailable() && player.getSubtitleSelectionState().activeSecondarySelection != null;
    }

    private boolean isAvailable() {
        return player != null && !player.isReleased() && player.supportsSubtitleOffsets();
    }
}
