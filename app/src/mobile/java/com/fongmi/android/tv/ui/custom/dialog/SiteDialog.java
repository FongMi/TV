package com.fongmi.android.tv.ui.custom.dialog;

import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.ui.adapter.SiteAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SiteDialog implements SiteAdapter.OnClickListener {

    private final DialogSiteBinding binding;
    private final SiteCallback callback;
    private final SiteAdapter adapter;
    private final AlertDialog dialog;

    public static SiteDialog create(Fragment fragment) {
        return new SiteDialog(fragment);
    }

    public SiteDialog(Fragment fragment) {
        this.callback = (fragment instanceof SiteCallback) ? (SiteCallback) fragment : null;
        this.binding = DialogSiteBinding.inflate(LayoutInflater.from(fragment.getContext()));
        this.dialog = new MaterialAlertDialogBuilder(fragment.getActivity()).setView(binding.getRoot()).create();
        this.adapter = new SiteAdapter(this);
    }

    public SiteDialog search(boolean search) {
        this.adapter.search(search);
        return this;
    }

    public SiteDialog filter(boolean filter) {
        this.adapter.filter(filter);
        return this;
    }

    public SiteDialog all() {
        this.adapter.search(true);
        this.adapter.filter(true);
        return this;
    }

    public void show() {
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 16));
        binding.recycler.scrollToPosition(ApiConfig.getHomeIndex());
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    @Override
    public void onTextClick(Site item) {
        if (callback == null) return;
        callback.setSite(item);
        dialog.dismiss();
    }

    @Override
    public void onSearchClick(Site item) {
        item.setSearchable(!item.isSearchable()).save();
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
    }

    @Override
    public void onFilterClick(Site item) {
        item.setFilterable(!item.isFilterable()).save();
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
    }

    @Override
    public boolean onSearchLongClick(Site item) {
        boolean result = !item.isSearchable();
        for (Site site : ApiConfig.get().getSites()) site.setSearchable(result).save();
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        return true;
    }

    @Override
    public boolean onFilterLongClick(Site item) {
        boolean result = !item.isFilterable();
        for (Site site : ApiConfig.get().getSites()) site.setFilterable(result).save();
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        return true;
    }
}
