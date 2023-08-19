package com.fongmi.android.tv.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.bean.Episode;
import com.fongmi.android.tv.databinding.FragmentEpisodeBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.base.ViewType;

import java.util.ArrayList;
import java.util.List;

public class EpisodeFragment extends BaseFragment implements EpisodeAdapter.OnClickListener {

    private FragmentEpisodeBinding mBinding;
    private SiteViewModel mViewModel;

    private int getSpanCount() {
        return getArguments().getInt("spanCount");
    }

    private ArrayList<Episode> getItems() {
        return getArguments().getParcelableArrayList("items");
    }

    public static EpisodeFragment newInstance(int spanCount, List<Episode> items) {
        Bundle args = new Bundle();
        args.putInt("spanCount", spanCount);
        args.putParcelableArrayList("items", new ArrayList<>(items));
        EpisodeFragment fragment = new EpisodeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentEpisodeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setRecyclerView();
        setViewModel();
    }

    private void setRecyclerView() {
        EpisodeAdapter adapter;
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setItemAnimator(null);
        mBinding.recycler.setLayoutManager(new GridLayoutManager(getContext(), getSpanCount()));
        mBinding.recycler.setAdapter(adapter = new EpisodeAdapter(this, ViewType.GRID, getItems()));
        mBinding.recycler.scrollToPosition(adapter.getPosition());
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(requireActivity()).get(SiteViewModel.class);
    }

    @Override
    public void onItemClick(Episode item) {
        mViewModel.setEpisode(item);
    }
}
