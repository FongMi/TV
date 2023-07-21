package com.fongmi.android.tv.ui.custom.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import com.fongmi.android.tv.databinding.DialogSubtitleBinding;
import com.fongmi.android.tv.impl.SubtitleCallback;
import com.fongmi.android.tv.ui.adapter.SubtitleAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SubtitleDialog implements SubtitleAdapter.OnClickListener {

    private final DialogSubtitleBinding binding;
    private final SubtitleCallback callback;
    private final SubtitleAdapter adapter;
    private final AlertDialog dialog;

    public static SubtitleDialog create(Activity activity) {
        return new SubtitleDialog(activity);
    }

    public SubtitleDialog(Activity activity) {
        this.callback = (SubtitleCallback) activity;
        this.binding = DialogSubtitleBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        this.adapter = new SubtitleAdapter(this);
    }

    public void show() {
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.scrollToPosition(Prefers.getSubtitle());
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
    public void onItemClick(int position) {
        callback.setSubtitle(position);
        dialog.dismiss();
    }
}
