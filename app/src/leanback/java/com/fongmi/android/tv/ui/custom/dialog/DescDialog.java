package com.fongmi.android.tv.ui.custom.dialog;

import android.app.Activity;
import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.databinding.DialogDescBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DescDialog {

    public static void show(Activity activity, String desc) {
        new DescDialog().create(activity, desc);
    }

    public void create(Activity activity, String desc) {
        DialogDescBinding binding = DialogDescBinding.inflate(LayoutInflater.from(activity));
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        dialog.getWindow().setDimAmount(0);
        binding.text.setText(desc);
        dialog.show();
    }
}
