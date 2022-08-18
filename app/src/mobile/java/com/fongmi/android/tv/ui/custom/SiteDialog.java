package com.fongmi.android.tv.ui.custom;

import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.ui.adapter.SiteAdapter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SiteDialog implements SiteAdapter.OnClickListener {

    private DialogSiteBinding binding;
    private SiteAdapter adapter;
    private AlertDialog dialog;
    private Callback callback;

    public static void show(Fragment fragment) {
        if (ApiConfig.get().getSites().isEmpty()) return;
        new SiteDialog().create(fragment);
    }

    public void create(Fragment fragment) {
        callback = (Callback) fragment;
        binding = DialogSiteBinding.inflate(LayoutInflater.from(fragment.getContext()));
        dialog = new MaterialAlertDialogBuilder(fragment.getContext()).setView(binding.getRoot()).create();
        initDialog();
        initView();
    }

    private void initDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.height = (int) (ResUtil.getScreenHeightPx() * 0.6f);
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void initView() {
        int position = ApiConfig.get().getSites().indexOf(ApiConfig.get().getHome());
        adapter = new SiteAdapter(this);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(adapter);
        binding.recycler.scrollToPosition(position);
    }

    @Override
    public void onItemClick(Site item) {
        callback.setSite(item);
        dialog.dismiss();
    }

    public interface Callback {

        void setSite(Site item);
    }
}
