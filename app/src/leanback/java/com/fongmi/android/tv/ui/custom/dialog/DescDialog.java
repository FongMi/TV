package com.fongmi.android.tv.ui.custom.dialog;

import android.app.Activity;
import android.text.Editable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.databinding.DialogDescBinding;
import com.github.bassaer.library.MDColor;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DescDialog {

    public static void show(Activity activity, CharSequence desc) {
        new DescDialog().create(activity, desc);
    }

    public void create(Activity activity, CharSequence desc) {
        DialogDescBinding binding = DialogDescBinding.inflate(LayoutInflater.from(activity));
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        dialog.getWindow().setDimAmount(0);
        initView(binding.text, desc);
        dialog.show();
    }

    private void initView(TextView view, CharSequence desc) {
        view.setLinkTextColor(MDColor.BLUE_500);
        view.setText(desc, TextView.BufferType.SPANNABLE);
        Editable e = new Editable.Factory().newEditable(view.getText());
        ClickableSpan[] spans = e.getSpans(0, e.length(), ClickableSpan.class);
        if (spans.length > 0) view.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
