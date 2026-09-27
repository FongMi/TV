package com.fongmi.android.tv.ui.dialog;

import static androidx.appcompat.R.attr.colorError;
import static com.google.android.material.R.attr.colorOnSurfaceVariant;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogMpvConfBinding;
import com.fongmi.android.tv.player.mpv.MpvConfigFile;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.List;

public class MpvConfDialog extends BaseAlertDialog {

    private static final String[] MPV_CONF_MIME_TYPES = new String[]{"text/*", "application/octet-stream", "*/*"};
    private static final String IMPORT = "Import";
    private static final String PRIORITY_HINT = "Player settings take priority; reopen playback to apply.";
    private static final String CONFLICT_HINT = "Player settings will override: %s";
    private static final String SAVE_FAILED = "Unable to save mpv.conf";
    private static final String IMPORT_SUCCESS = "Imported mpv.conf";
    private static final String IMPORT_FAILED = "Unable to import mpv.conf";

    private DialogMpvConfBinding binding;

    public static void show(Fragment fragment) {
        new MpvConfDialog().show(fragment.getChildFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogMpvConfBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setTitle(R.string.player_mpv_conf).setView(getBinding().getRoot()).setNeutralButton(IMPORT, null).setPositiveButton(R.string.dialog_positive, null).setNegativeButton(R.string.dialog_negative, null);
    }

    @Override
    protected void initView() {
        setText(MpvConfigFile.read());
        updateConflictHint(binding.text.getText());
    }

    @Override
    protected void initEvent() {
        binding.text.addTextChangedListener(new CustomTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateConflictHint(s);
            }
        });
    }

    private void setText(String text) {
        binding.text.setText(text);
        binding.text.setSelection(TextUtils.isEmpty(text) ? 0 : text.length());
    }

    private void updateConflictHint(CharSequence text) {
        List<String> conflicts = MpvConfigFile.findInterfaceManagedOptions(text);
        boolean hasConflicts = !conflicts.isEmpty();
        int color = hasConflicts ? colorError : colorOnSurfaceVariant;
        binding.hint.setTextColor(MaterialColors.getColor(binding.hint, color));
        String hint = hasConflicts ? String.format(CONFLICT_HINT, TextUtils.join(", ", conflicts)) : PRIORITY_HINT;
        binding.hint.setText(hint);
    }

    private void onPositive(View view) {
        if (MpvConfigFile.write(binding.text.getText().toString())) dismiss();
        else Notify.show(SAVE_FAILED);
    }

    private void onChoose(View view) {
        FileChooser.from(launcher).show("*/*", MPV_CONF_MIME_TYPES);
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> FileChooser.getUri(result, this::importConfig));

    private void importConfig(Uri uri) {
        if (!isAdded()) return;
        boolean success = MpvConfigFile.importFrom(uri);
        Notify.show(success ? IMPORT_SUCCESS : IMPORT_FAILED);
        if (success) setText(MpvConfigFile.read());
        try {
            setText(MpvConfigFile.read(uri));
            Notify.show(R.string.player_mpv_conf_import_success);
        } catch (IOException | SecurityException e) {
            Notify.show(R.string.player_mpv_conf_import_failed);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(this::onChoose);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(this::onPositive);
        }
    }
}
