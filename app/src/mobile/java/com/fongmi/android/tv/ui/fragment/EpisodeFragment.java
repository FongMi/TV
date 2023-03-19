package com.fongmi.android.tv.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentEpisodeBinding;
import com.fongmi.android.tv.ui.adapter.EpisodeAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.ViewType;
import com.google.gson.Gson;

import java.util.List;

public class EpisodeFragment extends BaseFragment implements EpisodeAdapter.OnClickListener {

    private FragmentEpisodeBinding mBinding;
    private EpisodeAdapter mAdapter;

    private String getJson() {
        return getArguments().getString("json");
    }

    public static EpisodeFragment newInstance(List<Vod.Flag.Episode> items) {
        Bundle args = new Bundle();
        args.putString("json", new Gson().toJson(items));
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
        setEpisode();
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setItemAnimator(null);
        mBinding.recycler.setAdapter(mAdapter = new EpisodeAdapter(this, ViewType.GRID));
    }

    private void setEpisode() {
        List<Vod.Flag.Episode> items = Vod.Flag.Episode.arrayFrom(getJson());
        mBinding.recycler.setLayoutManager(new GridLayoutManager(getContext(), getSpan(items)));
        mAdapter.addAll(items);
    }

    private int getSpan(List<Vod.Flag.Episode> items) {
        int total = 0;
        for (Vod.Flag.Episode item : items) total += item.getName().length();
        int offset = total / items.size();
        if (offset > 20) return 1;
        if (offset > 15) return 2;
        if (offset > 10) return 3;
        if (offset > 5) return 4;
        return 5;
    }

    @Override
    protected void initEvent() {

    }

    @Override
    public void onItemClick(Vod.Flag.Episode item) {

    }
}
