package com.fongmi.android.tv.ui.custom;

import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.ui.adapter.SiteAdapter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SiteDialog implements SiteAdapter.OnClickListener {

    private DialogSiteBinding binding;
    private SiteCallback callback;
    private SiteAdapter adapter;
    private AlertDialog dialog;

    public static void show(Fragment fragment) {
        if (ApiConfig.get().getSites().isEmpty()) return;
        new SiteDialog().create(fragment);
    }

    public void create(Fragment fragment) {
        callback = (SiteCallback) fragment;
        binding = DialogSiteBinding.inflate(LayoutInflater.from(fragment.getContext()));
        dialog = new MaterialAlertDialogBuilder(fragment.getActivity()).setView(binding.getRoot()).create();
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(adapter = new SiteAdapter(this));
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.scrollToPosition(ApiConfig.getHomeIndex());
    }

    private void setDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.height = (int) (ResUtil.getScreenHeightPx() * 0.615f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    @Override
    public void onTextClick(Site item) {
        callback.setSite(item);
        dialog.dismiss();
    }

    @Override
    public void onSearchClick(Site item) {
        item.setSearchable(!item.isSearchable()).save();
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onFilterClick(Site item) {
        item.setFilterable(!item.isFilterable()).save();
        adapter.notifyDataSetChanged();
    }
}
