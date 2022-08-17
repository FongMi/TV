package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.ui.presenter.SitePresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SiteDialog implements SitePresenter.OnClickListener {

    private ArrayObjectAdapter adapter;
    private DialogSiteBinding binding;
    private AlertDialog dialog;
    private Callback callback;

    public static void show(Activity activity) {
        if (ApiConfig.get().getSites().isEmpty()) return;
        new SiteDialog().create(activity);
    }

    public void create(Activity activity) {
        callback = (Callback) activity;
        binding = DialogSiteBinding.inflate(LayoutInflater.from(activity));
        dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        initDialog();
        initView();
    }

    private void initDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidthPx() * 0.45f);
        params.height = (int) (ResUtil.getScreenHeightPx() * 0.85f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        int position = ApiConfig.get().getSites().indexOf(ApiConfig.get().getHome());
        adapter = new ArrayObjectAdapter(new SitePresenter(this));
        adapter.addAll(0, ApiConfig.get().getSites());
        binding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        binding.recycler.setAdapter(new ItemBridgeAdapter(adapter));
        binding.recycler.scrollToPosition(position);
    }

    @Override
    public void onTextClick(Site item) {
        callback.setSite(item);
        dialog.dismiss();
    }

    @Override
    public void onSearchClick(Site item) {
        item.setSearchable(!item.isSearchable());
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
    }

    @Override
    public void onFilterClick(Site item) {
        item.setFilterable(!item.isFilterable());
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
    }

    public interface Callback {

        void setSite(Site site);
    }
}
