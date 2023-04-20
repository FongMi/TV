package com.fongmi.android.tv.ui.custom.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.databinding.DialogLiveBinding;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.ui.adapter.LiveAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class LiveDialog extends BaseDialog implements LiveAdapter.OnClickListener {

    private DialogLiveBinding binding;
    private LiveCallback callback;

    public static LiveDialog create() {
        return new LiveDialog();
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
        this.callback = (LiveCallback) activity;
    }

    public void show(Fragment fragment) {
        for (Fragment f : fragment.getChildFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(fragment.getChildFragmentManager(), null);
        this.callback = (LiveCallback) fragment;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogLiveBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setAdapter(new LiveAdapter(this));
        binding.recycler.addItemDecoration(new SpaceItemDecoration(2, 16));
        binding.recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.recycler.scrollToPosition(LiveConfig.getHomeIndex());
    }

    @Override
    public void onItemClick(Live item) {
        callback.setLive(item);
        dismiss();
    }
}
