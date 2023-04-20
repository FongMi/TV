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
    private final SitePresenter presenter;
    private final SiteCallback callback;
    private final AlertDialog dialog;
    private float width;

    public static SiteDialog create(Activity activity) {
        return new SiteDialog(activity);
    }

    public SiteDialog(Activity activity) {
        this.binding = DialogSiteBinding.inflate(LayoutInflater.from(activity));
        this.adapter = new ArrayObjectAdapter(presenter = new SitePresenter(this));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        this.callback = (SiteCallback) activity;
    }

    public SiteDialog search() {
        this.presenter.search(true);
        this.width = 0.4f;
        return this;
    }

    public SiteDialog change() {
        this.presenter.change(true);
        this.width = 0.4f;
        return this;
    }

    public SiteDialog all() {
        this.presenter.search(true);
        this.presenter.change(true);
        this.width = 0.45f;
        return this;
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
        params.width = (int) (ResUtil.getScreenWidth() * width);
        params.height = (int) (ResUtil.getScreenHeight() * 0.738f);
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
    public void onChangeClick(Site item) {
        item.setChangeable(!item.isChangeable()).save();
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
    public boolean onChangeLongClick(Site item) {
        boolean result = !item.isChangeable();
        for (Site site : ApiConfig.get().getSites()) site.setChangeable(result).save();
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
        return true;
    }
}
