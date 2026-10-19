package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.inputmethod.EditorInfo;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.databinding.DialogSingleInputBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DeviceNameDialog extends BaseAlertDialog {

    private DialogSingleInputBinding binding;
    private Device device;

    public static void show(DeviceConnectDialog parent, Device device) {
        if (parent.getChildFragmentManager().isStateSaved() || parent.getChildFragmentManager().findFragmentByTag(DeviceNameDialog.class.getSimpleName()) != null) return;
        Bundle args = new Bundle();
        args.putString("device", device.toString());
        DeviceNameDialog dialog = new DeviceNameDialog();
        dialog.setArguments(args);
        dialog.show(parent.getChildFragmentManager(), DeviceNameDialog.class.getSimpleName());
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogSingleInputBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setTitle(R.string.device_rename).setView(getBinding().getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, null);
    }

    @Override
    protected void initView() {
        device = Device.objectFrom(requireArguments().getString("device"));
        binding.input.setHint(device.getName());
        binding.text.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        binding.text.setText(device.getAlias());
        binding.text.setSelection(binding.text.length());
    }

    @Override
    protected void initEvent() {
        binding.text.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            onPositive(null, 0);
            return true;
        });
    }

    private void onPositive(DialogInterface dialog, int which) {
        device.setAlias(binding.text.getText().toString());
        ((DeviceConnectDialog) requireParentFragment()).refreshNames();
        dismiss();
    }
}
