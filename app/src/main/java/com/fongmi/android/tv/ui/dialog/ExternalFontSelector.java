package com.fongmi.android.tv.ui.dialog;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.player.mpv.MpvConfigFile;
import com.fongmi.android.tv.player.subtitle.ExternalFont;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.utils.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

final class ExternalFontSelector {

    private static final String[] MIME_TYPES = {"font/*", "application/x-font-ttf", "application/x-font-opentype", "application/octet-stream"};

    private final ActivityResultLauncher<Intent> launcher;
    private final Listener listener;
    private final Fragment fragment;
    private final boolean subtitles;

    @Nullable private ChipGroup group;
    @Nullable private AlertDialog faceDialog;
    @Nullable private ExternalFont.Item selected;
    @Nullable private List<ExternalFont.Entry> entries;
    private boolean progressVisible;
    private boolean loading;
    private int loadGeneration;

    static ExternalFontSelector forSubtitles(Fragment fragment, Listener listener) {
        return new ExternalFontSelector(fragment, listener, true);
    }

    ExternalFontSelector(Fragment fragment, Listener listener) {
        this(fragment, listener, false);
    }

    private ExternalFontSelector(Fragment fragment, Listener listener, boolean subtitles) {
        this.fragment = fragment;
        this.listener = listener;
        this.subtitles = subtitles;
        this.launcher = fragment.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> FileChooser.getUri(result, this::openFont));
    }

    void bind(ChipGroup group, @Nullable ExternalFont.Item selected) {
        this.group = group;
        this.selected = selected;
        loadFonts();
        render();
    }

    void release() {
        loadGeneration++;
        loading = false;
        group = null;
        entries = null;
        dismissFaceDialog();
        dismissProgress();
    }

    private void loadFonts() {
        if (entries != null || loading) return;
        loading = true;
        ExternalFont.Item selected = this.selected;
        int generation = loadGeneration;
        Task.execute(() -> readFonts(selected, generation));
    }

    private void readFonts(@Nullable ExternalFont.Item selected, int generation) {
        try {
            File directory = MpvConfigFile.getSubtitleFontsDirectory();
            List<ExternalFont.Entry> loaded = subtitles ? ExternalFont.getSubtitleEntries(directory, selected) : ExternalFont.getAll(directory);
            postResult(generation, () -> onLoaded(loaded));
        } catch (RuntimeException e) {
            postResult(generation, () -> onLoadFailed(e));
        }
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
        chip.setText(getFontLabel(font));
        if (entry != null && entry.typeface() != null) chip.setTypeface(entry.typeface());
        chip.setCheckable(true);
        chip.setOnClickListener(view -> select(font));
        group.addView(chip);
        if (Objects.equals(font, selected)) group.check(chip.getId());
    }

    private String getFontLabel(@Nullable ExternalFont.Item font) {
        if (font == null) return ResUtil.getString(R.string.playback_font_default);
        if (subtitles) return hasDuplicateLabel(font) ? font.displayName() + " · " + font.directory().getAbsolutePath() : font.displayName();
        if (font.source() == null) return font.fileName();
        return font.fileName() + (isConfiguredFont(font) ? " (mpv.conf)" : " (mpv/fonts)");
    }

    private boolean hasDuplicateLabel(ExternalFont.Item font) {
        return entries != null && entries.stream().anyMatch(entry -> !font.file().equals(entry.item().file()) && font.displayName().equals(entry.item().displayName()));
    }

    private void addImportChip(ChipGroup group) {
        Chip chip = createChip(group);
        chip.setText(subtitles ? R.string.playback_font_choose : R.string.playback_font_import);
        chip.setCheckable(false);
        chip.setEnabled(!loading);
        chip.setOnClickListener(this::onImport);
        group.addView(chip);
    }

    private Chip createChip(ChipGroup group) {
        Chip chip = (Chip) LayoutInflater.from(fragment.requireContext()).inflate(R.layout.view_filter_chip, group, false);
        chip.setId(View.generateViewId());
        return chip;
    }

    private void onImport(View view) {
        PermissionUtil.requestFile(fragment, granted -> {
            if (granted) FileChooser.from(launcher).show(MIME_TYPES);
        });
    }

    private static boolean isConfiguredFont(ExternalFont.Item font) {
        return font.source() != null && !font.directory().equals(ExternalFont.getMpvDirectory());
    }

    private void select(@Nullable ExternalFont.Item font) {
        selected = font;
        listener.onSelected(font);
    }

    private void openFont(Uri uri) {
        if (!fragment.isAdded() || loading) return;
        loading = true;
        render();
        progressVisible = true;
        Notify.progress(fragment.requireContext());
        int generation = loadGeneration;
        Task.execute(() -> {
            if (subtitles) readOriginalFont(uri, generation);
            else importFontFile(uri, generation);
        });
    }

    private void postResult(int generation, Runnable callback) {
        App.post(() -> {
            if (generation == loadGeneration) callback.run();
        });
    }

    private void readOriginalFont(Uri uri, int generation) {
        try {
            File file = FileChooser.getLocalFile(uri);
            if (file == null) throw new IOException(ResUtil.getString(R.string.error_file_open));
            List<ExternalFont.Entry> faces = ExternalFont.readSubtitleFile(file);
            postResult(generation, () -> onOriginalFontRead(faces));
        } catch (IOException | SecurityException e) {
            postResult(generation, () -> onImportFailed(e));
        }
    }

    private void onOriginalFontRead(List<ExternalFont.Entry> faces) {
        loading = false;
        dismissProgress();
        ChipGroup group = this.group;
        if (group == null || !fragment.isAdded()) return;
        if (faces.size() == 1) {
            selectNewFont(faces.get(0).item());
            return;
        }
        render();
        showFaceDialog(group, faces);
    }

    private void showFaceDialog(ChipGroup group, List<ExternalFont.Entry> faces) {
        CharSequence[] names = faces.stream().map(entry -> entry.item().displayName()).toArray(CharSequence[]::new);
        faceDialog = new MaterialAlertDialogBuilder(group.getContext()).setTitle(faces.get(0).item().fileName()).setItems(names, (dialog, which) -> selectNewFont(faces.get(which).item())).create();
        faceDialog.setOnDismissListener(dialog -> {
            if (faceDialog == dialog) faceDialog = null;
        });
        faceDialog.show();
    }

    private void dismissFaceDialog() {
        AlertDialog dialog = faceDialog;
        faceDialog = null;
        if (dialog != null) dialog.dismiss();
    }

    private void importFontFile(Uri uri, int generation) {
        try {
            ExternalFont.Item font = ExternalFont.importFrom(uri);
            postResult(generation, () -> onImported(font));
        } catch (Exception e) {
            postResult(generation, () -> onImportFailed(e));
        }
    }

    private void onImported(ExternalFont.Item font) {
        loading = false;
        dismissProgress();
        if (group == null || !fragment.isAdded()) return;
        Notify.show(ResUtil.getString(R.string.playback_font_imported, font.displayName()));
        selectNewFont(font);
    }

    private void selectNewFont(ExternalFont.Item font) {
        selected = font;
        entries = null;
        loadFonts();
        render();
        listener.onSelected(font);
    }

    @Nullable
    private ExternalFont.Item find(List<ExternalFont.Entry> entries, @Nullable ExternalFont.Item selected) {
        if (selected == null) return null;
        for (ExternalFont.Entry entry : entries) if (entry.item().sameFace(selected)) return entry.item();
        return null;
    }

    private void onImportFailed(Exception error) {
        loading = false;
        dismissProgress();
        if (group == null || !fragment.isAdded()) return;
        render();
        Notify.show(Notify.getError(subtitles ? R.string.playback_font_load_failed : R.string.playback_font_import_failed, error));
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
