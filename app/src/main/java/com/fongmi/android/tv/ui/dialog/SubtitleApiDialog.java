package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;

import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogSubtitleApiBinding;
import com.fongmi.android.tv.setting.SubtitleSetting;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SubtitleApiDialog extends BaseAlertDialog {

    private DialogSubtitleApiBinding binding;
    private Runnable onSaved;

    public static SubtitleApiDialog create() {
        return new SubtitleApiDialog();
    }

    public SubtitleApiDialog onSaved(Runnable onSaved) {
        this.onSaved = onSaved;
        return this;
    }

    public void show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogSubtitleApiBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setTitle(R.string.subtitle_search_api_title).setView(getBinding().getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, null);
    }

    @Override
    protected void initView() {
        String text;
        binding.text.setText(text = SubtitleSetting.getEffectiveToken());
        binding.text.setSelection(TextUtils.isEmpty(text) ? 0 : text.length());
    }

    @Override
    protected void initEvent() {
        binding.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) onPositive(null, 0);
            return true;
        });
    }

    private void onPositive(DialogInterface dialog, int which) {
        CharSequence text = binding.text.getText();
        String token = text == null ? "" : text.toString().trim();
        SubtitleSetting.putSearchToken(token);
        if (!TextUtils.isEmpty(SubtitleSetting.getEffectiveToken()) && onSaved != null) App.post(onSaved);
        dismiss();
    }
}
