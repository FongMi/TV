package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogInfoBinding;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Map;

public class InfoDialog {

    private final DialogInfoBinding binding;
    private final Listener callback;
    private AlertDialog dialog;
    private CharSequence title;
    private String header;
    private String url;

    public static InfoDialog create(Activity activity) {
        return new InfoDialog(activity);
    }

    public InfoDialog(Activity activity) {
        this.binding = DialogInfoBinding.inflate(LayoutInflater.from(activity));
        this.callback = (Listener) activity;
    }

    public InfoDialog title(CharSequence title) {
        this.title = title;
        return this;
    }

    public InfoDialog headers(Map<String, String> headers) {
        StringBuilder sb = new StringBuilder();
        for (String key : headers.keySet()) sb.append(key).append(" : ").append(headers.get(key)).append("\n");
        this.header = Util.substring(sb.toString());
        return this;
    }

    public InfoDialog url(String url) {
        this.url = url;
        return this;
    }

    public void show() {
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        dialog = new MaterialAlertDialogBuilder(binding.getRoot().getContext()).setView(binding.getRoot()).create();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        binding.url.setText(url);
        binding.title.setText(title);
        binding.header.setText(header);
        binding.header.setVisibility(header.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void initEvent() {
        binding.url.setOnClickListener(this::onShare);
        binding.url.setOnLongClickListener(this::onCopy);
    }

    private void onShare(View view) {
        callback.onShare(title, url);
        dialog.dismiss();
    }

    private boolean onCopy(View view) {
        Notify.show(R.string.copied);
        Util.copy(url);
        return true;
    }

    public interface Listener {

        void onShare(CharSequence title, String url);
    }
}
