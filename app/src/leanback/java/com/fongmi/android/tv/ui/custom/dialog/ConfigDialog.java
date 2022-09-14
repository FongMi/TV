package com.fongmi.android.tv.ui.custom.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogConfigBinding;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.QRCode;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ConfigDialog implements DialogInterface.OnDismissListener {

    private DialogConfigBinding binding;
    private ConfigCallback callback;
    private AlertDialog dialog;

    public static void show(Activity activity) {
        new ConfigDialog().create(activity);
    }

    public void create(Activity activity) {
        callback = (ConfigCallback) activity;
        binding = DialogConfigBinding.inflate(LayoutInflater.from(activity));
        dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        EventBus.getDefault().register(this);
        initDialog();
        initView();
        initEvent();
    }

    private void initDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidthPx() * 0.6f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.setOnDismissListener(this);
        dialog.show();
    }

    private void initView() {
        String address = Server.get().getAddress(false);
        binding.text.setText(Prefers.getUrl());
        binding.text.setSelection(binding.text.getText().length());
        binding.code.setImageBitmap(QRCode.getBitmap(address, 200, 0));
        binding.info.setText(ResUtil.getString(R.string.push_info, address).replace("，", "\n"));
    }

    private void initEvent() {
        binding.positive.setOnClickListener(this::onPositive);
        binding.negative.setOnClickListener(this::onNegative);
        binding.text.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) binding.positive.performClick();
            return true;
        });
    }

    private void onPositive(View view) {
        String url = binding.text.getText().toString().trim();
        if (url.startsWith("clan")) url = url.replace("clan", "file");
        callback.setConfig(url);
        dialog.dismiss();
    }

    private void onNegative(View view) {
        dialog.dismiss();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        if (event.getType() != ServerEvent.Type.API) return;
        binding.text.setText(event.getText());
        binding.text.setSelection(binding.text.getText().length());
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        EventBus.getDefault().unregister(this);
    }
}
