package com.fongmi.android.tv.ui.dialog;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.player.subtitle.ExternalFont;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

final class ExternalFontSelector {

    private static final String[] MIME_TYPES = {"font/*", "application/x-font-ttf", "application/x-font-opentype", "application/octet-stream"};

    private final ActivityResultLauncher<Intent> launcher;
    private final Listener listener;
    private final Fragment fragment;

    @Nullable private ChipGroup group;
    @Nullable private ExternalFont.Item selected;
    @Nullable private List<ExternalFont.Entry> entries;
    private boolean progressVisible;
    private boolean loading;

    ExternalFontSelector(Fragment fragment, Listener listener) {
        this.fragment = fragment;
        this.listener = listener;
        this.launcher = fragment.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> FileChooser.getUri(result, this::importFont));
    }

    void bind(ChipGroup group, @Nullable ExternalFont.Item selected) {
        this.group = group;
        this.selected = selected;
        loadFonts();
        render();
    }

    void release() {
        group = null;
        entries = null;
        dismissProgress();
    }

    private void loadFonts() {
        if (entries != null || loading) return;
        loading = true;
        Task.execute(() -> {
            try {
                List<ExternalFont.Entry> loaded = ExternalFont.getAll();
                App.post(() -> onLoaded(loaded));
            } catch (RuntimeException e) {
                App.post(() -> onLoadFailed(e));
            }
        });
    }

    private void onLoaded(List<ExternalFont.Entry> entries) {
        loading = false;
        if (group == null) return;
        this.entries = List.copyOf(entries);
        this.selected = find(entries, selected);
        render();
    }

    private void render() {
        ChipGroup group = this.group;
        if (group == null || !fragment.isAdded()) return;
        boolean restoreFocus = group.hasFocus();
        Chip selectedChip = renderChips(group);
        if (restoreFocus) selectedChip.requestFocus();
    }

    private Chip renderChips(ChipGroup group) {
        List<ExternalFont.Entry> fonts = entries == null ? initialEntries() : entries;
        group.removeAllViews();
        addFontChip(group, null);
        fonts.forEach(entry -> addFontChip(group, entry));
        addImportChip(group);
        return group.findViewById(group.getCheckedChipId());
    }

    private List<ExternalFont.Entry> initialEntries() {
        return selected == null ? List.of() : List.of(new ExternalFont.Entry(selected, null));
    }

    private void addFontChip(ChipGroup group, @Nullable ExternalFont.Entry entry) {
        ExternalFont.Item font = entry == null ? null : entry.item();
        Chip chip = createChip(group);
        chip.setText(font == null ? ResUtil.getString(R.string.playback_font_default) : font.displayName());
        if (entry != null && entry.typeface() != null) chip.setTypeface(entry.typeface());
        chip.setCheckable(true);
        chip.setOnClickListener(view -> select(font));
        group.addView(chip);
        if (font == null || font.equals(selected)) group.check(chip.getId());
    }

    private void addImportChip(ChipGroup group) {
        Chip chip = createChip(group);
        chip.setText(R.string.playback_font_import);
        chip.setCheckable(false);
        chip.setEnabled(!loading);
        chip.setOnClickListener(view -> FileChooser.from(launcher).show(MIME_TYPES));
        group.addView(chip);
    }

    private Chip createChip(ChipGroup group) {
        Chip chip = (Chip) LayoutInflater.from(fragment.requireContext()).inflate(R.layout.view_filter_chip, group, false);
        chip.setId(View.generateViewId());
        return chip;
    }

    private void select(@Nullable ExternalFont.Item font) {
        selected = font;
        listener.onSelected(font);
    }

    private void importFont(Uri uri) {
        if (!fragment.isAdded() || loading) return;
        loading = true;
        render();
        progressVisible = true;
        Notify.progress(fragment.requireContext());
        Task.execute(() -> {
            try {
                ExternalFont.Item font = ExternalFont.importFrom(uri);
                App.post(() -> onImported(font));
            } catch (Exception e) {
                App.post(() -> onImportFailed(e));
            }
        });
    }

    private void onImported(ExternalFont.Item font) {
        loading = false;
        dismissProgress();
        if (group == null || !fragment.isAdded()) return;
        Notify.show(ResUtil.getString(R.string.playback_font_imported, font.displayName()));
        selected = font;
        entries = null;
        loadFonts();
        render();
        listener.onSelected(font);
    }

    @Nullable
    private ExternalFont.Item find(List<ExternalFont.Entry> entries, @Nullable ExternalFont.Item selected) {
        if (selected == null) return null;
        for (ExternalFont.Entry entry : entries) if (entry.item().fileName().equals(selected.fileName())) return entry.item();
        return null;
    }

    private void onImportFailed(Exception error) {
        loading = false;
        dismissProgress();
        if (group == null || !fragment.isAdded()) return;
        render();
        Notify.show(Notify.getError(R.string.playback_font_import_failed, error));
    }

    private void dismissProgress() {
        if (!progressVisible) return;
        progressVisible = false;
        Notify.dismiss();
    }

    private void onLoadFailed(RuntimeException error) {
        loading = false;
        if (group == null || !fragment.isAdded()) return;
        render();
        Notify.show(Notify.getError(R.string.playback_font_load_failed, error));
    }

    interface Listener {

        void onSelected(@Nullable ExternalFont.Item font);
    }
}
