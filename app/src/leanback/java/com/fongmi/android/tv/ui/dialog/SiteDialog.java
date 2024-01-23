package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.ui.adapter.SiteAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SiteDialog implements SiteAdapter.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private RecyclerView.ItemDecoration decoration;
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
        return this;
    }

    public SiteDialog action() {
        binding.action.setVisibility(View.VISIBLE);
        return this;
    }

    public void show() {
        setType(type);
        initView();
        initEvent();
    }

    private boolean list() {
        return Setting.getSiteMode() == 0 || adapter.getItemCount() < 20;
    }

    private int getCount() {
        return list() ? 1 : Math.max(1, Math.min((int) (Math.ceil(adapter.getItemCount() / 20.0f)), 3));
    }

    private int getIcon() {
        return list() ? R.drawable.ic_site_grid : R.drawable.ic_site_list;
    }

    private float getWidth() {
        return 0.4f + (getCount() - 1) * 0.2f;
    }

    private void initView() {
        setRecyclerView();
        setDialog();
        setMode();
    }

    private void initEvent() {
        binding.check.setOnCheckedChangeListener(this);
        binding.mode.setOnClickListener(this::setMode);
        binding.search.setOnClickListener(v -> setType(v.isActivated() ? 0 : 1));
        binding.change.setOnClickListener(v -> setType(v.isActivated() ? 0 : 2));
        binding.record.setOnClickListener(v -> setType(v.isActivated() ? 0 : 3));
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        if (decoration != null) binding.recycler.removeItemDecoration(decoration);
        binding.recycler.addItemDecoration(decoration = new SpaceItemDecoration(getCount(), 16));
        binding.recycler.setLayoutManager(new GridLayoutManager(dialog.getContext(), getCount()));
        if (!binding.mode.hasFocus()) binding.recycler.post(() -> binding.recycler.scrollToPosition(VodConfig.getHomeIndex()));
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * getWidth());
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void setMode() {
        if (adapter.getItemCount() < 20) Setting.putSiteMode(0);
        binding.mode.setEnabled(adapter.getItemCount() >= 20);
        binding.mode.setImageResource(getIcon());
    }

    private void setType(int type) {
        binding.search.setActivated(type == 1);
        binding.change.setActivated(type == 2);
        binding.record.setActivated(type == 3);
        adapter.setType(this.type = type);
    }

    private void setMode(View view) {
        Setting.putSiteMode(Math.abs(Setting.getSiteMode() - 1));
        initView();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (type == 0) buttonView.setChecked(!isChecked);
        else if (isChecked) adapter.selectAll();
        else adapter.cancelAll();
    }

    @Override
    public void onItemClick(Site item) {
        callback.setSite(item);
        dialog.dismiss();
    }
}
