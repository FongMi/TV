package com.fongmi.android.tv.ui.custom.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.ui.adapter.SiteAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SiteDialog implements SiteAdapter.OnClickListener {

    private final DialogSiteBinding binding;
    private final SiteCallback callback;
    private final SiteAdapter adapter;
    private final AlertDialog dialog;

    public static SiteDialog create(Activity activity) {
        return new SiteDialog(activity);
    }

    public SiteDialog(Activity activity) {
        this.adapter = new SiteAdapter(this);
        this.callback = (SiteCallback) activity;
        this.binding = DialogSiteBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
    }

    public void show() {
        setRecyclerView();
        setDialog();
    }

    private int getSpanCount() {
        return adapter.getItemCount() >= 12 ? 2 : 1;
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(getSpanCount(), 16));
        binding.recycler.setLayoutManager(new GridLayoutManager(dialog.getContext(), getSpanCount()));
        binding.recycler.scrollToPosition(ApiConfig.getHomeIndex());
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * getSpanCount() * 0.4f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    @Override
    public void onItemClick(Site item) {
        callback.setSite(item);
        dialog.dismiss();
    }
}
