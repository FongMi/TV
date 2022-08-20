package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fongmi.android.tv.SettingCallback;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.DialogConfigHistoryBinding;
import com.fongmi.android.tv.ui.adapter.ConfigAdapter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ConfigHistoryDialog implements ConfigAdapter.OnClickListener {

    private DialogConfigHistoryBinding binding;
    private SettingCallback callback;
    private ConfigAdapter adapter;
    private AlertDialog dialog;

    public static void show(Activity activity) {
        new ConfigHistoryDialog().create(activity);
    }

    public void create(Activity activity) {
        callback = (SettingCallback) activity;
        binding = DialogConfigHistoryBinding.inflate(LayoutInflater.from(activity));
        dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setLayoutManager(new LinearLayoutManager(dialog.getContext()));
        binding.recycler.setAdapter(adapter = new ConfigAdapter(this));
    }

    private void setDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidthPx() * 0.45f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    @Override
    public void onTextClick(Config item) {
        callback.setConfig(item.getUrl());
        dialog.dismiss();
    }

    @Override
    public void onDeleteClick(Config item) {
        if (adapter.remove(item) == 0) dialog.dismiss();
    }
}
