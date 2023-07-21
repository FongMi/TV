package com.fongmi.android.tv.ui.custom.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
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
    private int type;

    public static SiteDialog create(Activity activity) {
        return new SiteDialog(activity);
    }

    public SiteDialog(Activity activity) {
        this.adapter = new SiteAdapter(this);
        this.callback = (SiteCallback) activity;
        this.binding = DialogSiteBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
    }

    public SiteDialog search() {
        type = 1;
        action();
        return this;
    }

    public SiteDialog action() {
        binding.action.setVisibility(View.VISIBLE);
        return this;
    }

    public void show() {
        binding.search.setOnClickListener(v -> setType(v.isActivated() ? 0 : 1));
        binding.change.setOnClickListener(v -> setType(v.isActivated() ? 0 : 2));
        binding.record.setOnClickListener(v -> setType(v.isActivated() ? 0 : 3));
        binding.select.setOnClickListener(v -> adapter.selectAll());
        binding.cancel.setOnClickListener(v -> adapter.cancelAll());
        setRecyclerView();
        setType(type);
        setDialog();
    }

    private int getSpanCount() {
        return Math.max(1, Math.min(adapter.getItemCount() / 20, 3));
    }

    private float getWidth() {
        return 0.4f + (getSpanCount() - 1) * 0.2f;
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.getItemAnimator().setChangeDuration(0);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(getSpanCount(), 16));
        binding.recycler.setLayoutManager(new GridLayoutManager(dialog.getContext(), getSpanCount()));
        binding.recycler.scrollToPosition(ApiConfig.getHomeIndex());
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * getWidth());
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void setType(int type) {
        binding.search.setActivated(type == 1);
        binding.change.setActivated(type == 2);
        binding.record.setActivated(type == 3);
        adapter.setType(type);
    }

    @Override
    public void onItemClick(Site item) {
        callback.setSite(item);
        dialog.dismiss();
    }
}
