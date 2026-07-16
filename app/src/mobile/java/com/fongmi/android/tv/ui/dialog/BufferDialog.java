package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;

import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogBufferBinding;
import com.fongmi.android.tv.impl.BufferListener;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.utils.SliderUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class BufferDialog extends BaseAlertDialog {

    private DialogBufferBinding binding;
    private int value;

    public static void show(Fragment fragment) {
        new BufferDialog().show(fragment.getChildFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogBufferBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setTitle(R.string.player_buffer).setView(getBinding().getRoot()).setPositiveButton(R.string.dialog_positive, this::onPositive).setNegativeButton(R.string.dialog_negative, this::onNegative);
    }

    @Override
    protected void initView() {
        SliderUtil.setValue(binding.slider, value = PlayerSetting.getBuffer());
    }

    private void onPositive(DialogInterface dialog, int which) {
        ((BufferListener) requireParentFragment()).setBuffer(Math.round(SliderUtil.snap(binding.slider, binding.slider.getValue())));
    }

    private void onNegative(DialogInterface dialog, int which) {
        ((BufferListener) requireParentFragment()).setBuffer(value);
    }
}
