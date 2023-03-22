package com.fongmi.android.tv.ui.custom.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.databinding.DialogLiveBinding;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.ui.adapter.LiveAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LiveDialog implements LiveAdapter.OnClickListener {

    private final DialogLiveBinding binding;
    private final LiveCallback callback;
    private final AlertDialog dialog;
    private final LiveAdapter adapter;

    public static LiveDialog create(Activity activity) {
        return new LiveDialog(activity);
    }

    public LiveDialog(Activity activity) {
        this.callback = (LiveCallback) activity;
        this.binding = DialogLiveBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        this.adapter = new LiveAdapter(this);
    }

    public void show() {
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.scrollToPosition(LiveConfig.getHomeIndex());
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * 0.4f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    @Override
    public void onItemClick(Live item) {
        callback.setLive(item);
        dialog.dismiss();
    }
}
