package com.fongmi.android.tv.ui.custom.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.ui.adapter.SiteAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class SiteDialog implements SiteAdapter.OnClickListener, ChipGroup.OnCheckedStateChangeListener {

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
        binding.group.check(R.id.search);
        this.type = 1;
        return this;
    }

    public void show() {
        binding.select.setOnClickListener(v -> adapter.selectAll());
        binding.cancel.setOnClickListener(v -> adapter.cancelAll());
        binding.group.setOnCheckedStateChangeListener(this);
        setRecyclerView();
        setType(type);
        setDialog();
    }

    private int getSpanCount() {
        return adapter.getItemCount() >= 12 ? 2 : 1;
    }

    private float getWidth() {
        return getSpanCount() == 2 ? 0.6f : 0.4f;
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
        binding.select.setVisibility(type == 0 ? View.GONE : View.VISIBLE);
        binding.cancel.setVisibility(type == 0 ? View.GONE : View.VISIBLE);
        adapter.setType(type);
    }

    @Override
    public void onItemClick(Site item) {
        callback.setSite(item);
        dialog.dismiss();
    }

    @Override
    public void onCheckedChanged(@NonNull ChipGroup group, @NonNull List<Integer> checkedIds) {
        if (checkedIds.size() == 0) setType(0);
        else if (checkedIds.get(0) == R.id.search) setType(1);
        else if (checkedIds.get(0) == R.id.change) setType(2);
        else if (checkedIds.get(0) == R.id.record) setType(3);
    }
}
