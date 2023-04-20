package com.fongmi.android.tv.ui.custom.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.ui.adapter.SiteAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SiteDialog extends BaseDialog implements SiteAdapter.OnClickListener {

    private final SiteAdapter adapter;
    private DialogSiteBinding binding;
    private SiteCallback callback;
    private int count;

    public static SiteDialog create() {
        return new SiteDialog();
    }

    public SiteDialog() {
        this.adapter = new SiteAdapter(this);
    }

    public SiteDialog search() {
        this.adapter.search(true);
        this.count = 2;
        return this;
    }

    public SiteDialog change() {
        this.adapter.change(true);
        this.count = 2;
        return this;
    }

    public SiteDialog all() {
        this.adapter.search(true);
        this.adapter.change(true);
        this.count = 1;
        return this;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
        this.callback = (SiteCallback) activity;
    }

    public void show(Fragment fragment) {
        for (Fragment f : fragment.getChildFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(fragment.getChildFragmentManager(), null);
        this.callback = (SiteCallback) fragment;
    }

    private int getCount() {
        return adapter.getItemCount() < 2 ? 1 : count;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogSiteBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(getCount(), 16));
        binding.recycler.setLayoutManager(new GridLayoutManager(getContext(), getCount()));
        binding.recycler.scrollToPosition(ApiConfig.getHomeIndex());
    }

    @Override
    public void onTextClick(Site item) {
        if (callback == null) return;
        callback.setSite(item);
        dismiss();
    }

    @Override
    public void onSearchClick(int position, Site item) {
        item.setSearchable(!item.isSearchable()).save();
        adapter.notifyItemChanged(position);
        callback.onChanged();
    }

    @Override
    public void onChangeClick(int position, Site item) {
        item.setChangeable(!item.isChangeable()).save();
        adapter.notifyItemChanged(position);
    }

    @Override
    public boolean onSearchLongClick(Site item) {
        boolean result = !item.isSearchable();
        for (Site site : ApiConfig.get().getSites()) site.setSearchable(result).save();
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        callback.onChanged();
        return true;
    }

    @Override
    public boolean onChangeLongClick(Site item) {
        boolean result = !item.isChangeable();
        for (Site site : ApiConfig.get().getSites()) site.setChangeable(result).save();
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        return true;
    }
}
