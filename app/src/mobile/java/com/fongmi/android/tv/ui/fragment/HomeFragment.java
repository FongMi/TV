package com.fongmi.android.tv.ui.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentHomeBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.BaseFragment;
import com.fongmi.android.tv.ui.adapter.HistoryAdapter;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;

public class HomeFragment extends BaseFragment implements VodAdapter.OnClickListener, HistoryAdapter.OnClickListener {

    private FragmentHomeBinding mBinding;
    private HistoryAdapter mHistoryAdapter;
    private SiteViewModel mViewModel;
    private VodAdapter mVodAdapter;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentHomeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setRecyclerView();
        setViewModel();
        getHistory();
        getVideo();
    }

    @Override
    protected void initEvent() {

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

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(getViewLifecycleOwner(), result -> {
            if (result != null) mVodAdapter.addAll(result.getList());
        });
    }

    private void getVideo() {
        if (ApiConfig.get().getHome().getKey().isEmpty()) return;
        mViewModel.homeContent();
    }

    private void getHistory() {
        mHistoryAdapter.addAll(History.get());
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
}
