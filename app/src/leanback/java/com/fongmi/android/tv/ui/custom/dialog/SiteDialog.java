package com.fongmi.android.tv.ui.custom.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.ui.presenter.SitePresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SiteDialog implements SitePresenter.OnClickListener {

    private final ArrayObjectAdapter adapter;
    private final DialogSiteBinding binding;
    private final SiteCallback callback;
    private final AlertDialog dialog;

    public static SiteDialog create(Activity activity) {
        return new SiteDialog(activity);
    }

    public SiteDialog(Activity activity) {
        this.callback = (SiteCallback) activity;
        this.binding = DialogSiteBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        this.adapter = new ArrayObjectAdapter(new SitePresenter(this));
    }

    public void show() {
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        adapter.addAll(0, ApiConfig.get().getSites());
        binding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        binding.recycler.setAdapter(new ItemBridgeAdapter(adapter));
        binding.recycler.scrollToPosition(ApiConfig.getHomeIndex());
    }

    private void setDialog() {
        if (adapter.size() == 0) return;
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidthPx() * 0.4f);
        params.height = (int) (ResUtil.getScreenHeightPx() * 0.74f);
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
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
    }

    @Override
    public void onFilterClick(Site item) {
        item.setFilterable(!item.isFilterable()).save();
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
    }

    @Override
    public boolean onSearchLongClick(Site item) {
        boolean result = !item.isSearchable();
        for (Site site : ApiConfig.get().getSites()) site.setSearchable(result).save();
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
        return true;
    }

    @Override
    public boolean onFilterLongClick(Site item) {
        boolean result = !item.isFilterable();
        for (Site site : ApiConfig.get().getSites()) site.setFilterable(result).save();
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
        return true;
    }
}
