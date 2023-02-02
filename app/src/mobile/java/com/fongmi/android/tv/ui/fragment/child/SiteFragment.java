package com.fongmi.android.tv.ui.fragment.child;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentSiteBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.activity.BaseFragment;
import com.fongmi.android.tv.ui.adapter.HistoryAdapter;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.google.gson.Gson;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class SiteFragment extends BaseFragment implements VodAdapter.OnClickListener, HistoryAdapter.OnClickListener {

    private FragmentSiteBinding mBinding;
    private HistoryAdapter mHistoryAdapter;
    private VodAdapter mVodAdapter;

    private String getList() {
        return getArguments().getString("list");
    }

    public static SiteFragment newInstance(List<Vod> list) {
        Bundle args = new Bundle();
        args.putString("list", new Gson().toJson(list));
        SiteFragment fragment = new SiteFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSiteBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setRecyclerView();
        getHistory();
        getVideo();
    }

    private void setRecyclerView() {
        mBinding.history.setHasFixedSize(true);
        mBinding.history.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mBinding.history.addItemDecoration(new SpaceItemDecoration(3, 16));
        mBinding.history.setAdapter(mHistoryAdapter = new HistoryAdapter(this));
        mBinding.recommend.setHasFixedSize(true);
        mBinding.recommend.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mBinding.recommend.addItemDecoration(new SpaceItemDecoration(3, 16));
        mBinding.recommend.setAdapter(mVodAdapter = new VodAdapter(this));
    }

    private void getHistory() {
        mHistoryAdapter.addAll(History.get());
    }

    private void getVideo() {
        mVodAdapter.addAll(Vod.arrayFrom(getList()));
    }

    @Override
    public void onItemClick(Vod item) {

    }

    @Override
    public boolean onLongClick(Vod item) {
        return true;
    }

    @Override
    public void onItemClick(History item) {
    }

    @Override
    public boolean onLongClick(History item) {
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case IMAGE:
                mVodAdapter.notifyItemRangeChanged(0, mVodAdapter.getItemCount());
                break;
            case HISTORY:
                getHistory();
                break;
        }
    }
}
