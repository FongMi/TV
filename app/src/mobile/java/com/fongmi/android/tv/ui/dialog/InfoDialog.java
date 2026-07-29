package com.fongmi.android.tv.ui.dialog;

import android.text.TextUtils;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogInfoBinding;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

public class InfoDialog extends BaseAlertDialog {

    private final Map<String, String> headers = new LinkedHashMap<>();
    private DialogInfoBinding binding;
    private String title = "";
    private String url = "";

    public static InfoDialog create(PlayerManager player) {
        InfoDialog dialog = new InfoDialog();
        String url = player.getUrl();
        dialog.title = player.getMediaTitle();
        dialog.headers.putAll(player.getHeaders());
        dialog.url = TextUtils.isEmpty(url) ? "" : url;
        return dialog;
    }

    public void show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = DialogInfoBinding.inflate(getLayoutInflater());
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        String url, header;
        binding.title.setText(title);
        binding.url.setText(url = buildUrl());
        binding.header.setText(header = buildHeader());
        binding.title.setSingleLine(title.contains(url));
        binding.url.setVisibility(TextUtils.isEmpty(url) ? View.GONE : View.VISIBLE);
        binding.header.setVisibility(TextUtils.isEmpty(header) ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void initEvent() {
        binding.url.setOnClickListener(this::onShare);
        binding.url.setOnLongClickListener(v -> onCopy(url));
        binding.header.setOnLongClickListener(v -> onCopy(binding.header.getText().toString()));
    }

    private void onShare(View view) {
        ((Listener) requireActivity()).onShare(title, url, headers);
        dismiss();
    }

    private boolean onCopy(String text) {
        Util.copy(text);
        return true;
    }

    private String buildUrl() {
        return url.startsWith("data") ? url.substring(0, Math.min(url.length(), 128)).concat("...") : url;
    }

    private String buildHeader() {
        if (headers.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String key : headers.keySet()) sb.append(key).append(" : ").append(headers.get(key)).append("\n");
        return Util.substring(sb.toString());
    }

    public interface Listener {

        void onShare(CharSequence title, String url, Map<String, String> headers);
    }
}
