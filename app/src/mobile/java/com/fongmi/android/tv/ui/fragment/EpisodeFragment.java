package com.fongmi.android.tv.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentEpisodeBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.base.ViewType;

import java.util.List;

public class EpisodeFragment extends BaseFragment implements EpisodeAdapter.OnClickListener {

    private FragmentEpisodeBinding mBinding;
    private SiteViewModel mViewModel;
    private EpisodeAdapter mAdapter;

    private int getSpanCount() {
        return getArguments().getInt("spanCount");
    }

    private String getJson() {
        return getArguments().getString("json");
    }

    public static EpisodeFragment newInstance(int spanCount, List<Vod.Flag.Episode> items) {
        Bundle args = new Bundle();
        args.putInt("spanCount", spanCount);
        args.putString("json", App.gson().toJson(items));
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
        setEpisode();
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setItemAnimator(null);
        mBinding.recycler.setAdapter(mAdapter = new EpisodeAdapter(this, ViewType.GRID));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(requireActivity()).get(SiteViewModel.class);
    }

    private void setEpisode() {
        mAdapter.addAll(Vod.Flag.Episode.arrayFrom(getJson()));
        mBinding.recycler.setLayoutManager(new GridLayoutManager(getContext(), getSpanCount()));
        mBinding.recycler.scrollToPosition(mAdapter.getPosition());
    }

    @Override
    public void onItemClick(Vod.Flag.Episode item) {
        mViewModel.setEpisode(item);
    }
}
