package com.fongmi.android.tv.ui.dialog;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.databinding.DialogUpdateBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;

public class UpdateDialog extends BaseAlertDialog {

    public static final String TAG = "update";

    private DialogUpdateBinding binding;

    public static UpdateDialog create() {
        UpdateDialog dialog = new UpdateDialog();
        dialog.setArguments(new Bundle());
        return dialog;
    }

    public UpdateDialog title(String title) {
        requireArguments().putString("title", title);
        return this;
    }

    public UpdateDialog desc(String desc) {
        requireArguments().putString("desc", desc);
        return this;
    }

    public UpdateDialog show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), TAG);
        return this;
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogUpdateBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setView(getBinding().getRoot()).setCancelable(false);
    }

    @Override
    protected void initView() {
        binding.version.setText(requireArguments().getString("title"));
        binding.desc.setText(requireArguments().getString("desc"));
    }

    @Override
    protected void initEvent() {
        binding.confirm.setOnClickListener(Updater.get()::onConfirm);
        binding.cancel.setOnClickListener(Updater.get()::onCancel);
    }

    @Override
    public void onStart() {
        super.onStart();
        Updater.get().attach(this);
    }

    @Override
    public void onStop() {
        Updater.get().detach(this);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    public void setState(boolean downloading, int progress, boolean ready) {
        if (binding == null) return;
        boolean verifying = downloading && (progress < 0 || progress >= 100);
        String text = getString(ready ? R.string.update_install : R.string.update_confirm);
        if (Updater.get().isPaused()) text = getString(R.string.transfer_resume);
        if (downloading) text = verifying ? getString(R.string.update_verifying) : NumberFormat.getPercentInstance().format(progress / 100.0);
        binding.confirm.setText(text);
        binding.confirm.setContentDescription(downloading && !verifying ? text + " · " + getString(R.string.pause) : text);
        binding.confirm.setEnabled(!verifying);
        binding.cancel.setText(downloading ? R.string.update_background : R.string.dialog_negative);
    }
}
