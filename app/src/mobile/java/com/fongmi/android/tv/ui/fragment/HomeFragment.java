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
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.BaseFragment;
import com.fongmi.android.tv.ui.adapter.HistoryAdapter;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;

public class HomeFragment extends BaseFragment implements VodAdapter.OnClickListener, HistoryAdapter.OnClickListener {

    private FragmentHomeBinding mBinding;
    private SiteViewModel mSiteViewModel;
    private HistoryAdapter mHistoryAdapter;
    private VodAdapter mRecommendAdapter;

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
        mBinding.recommend.setAdapter(mRecommendAdapter = new VodAdapter(this));
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.result.observe(getViewLifecycleOwner(), result -> {
            if (result != null) mRecommendAdapter.addAll(result.getList());
        });
    }

    private void getVideo() {
        if (ApiConfig.get().getHome().getKey().isEmpty()) return;
        mSiteViewModel.homeContent();
    }

    private void getHistory() {
        mHistoryAdapter.addAll(AppDatabase.get().getHistoryDao().getAll());
    }

    @Override
    public void onItemClick(Vod item) {
    }

    @Override
    public void onItemClick(History item) {
    }

    @Override
    public void onItemDelete(History item) {

    }

    @Override
    public boolean onLongClick() {
        mHistoryAdapter.setDelete(true);
        mHistoryAdapter.notifyDataSetChanged();
        return true;
    }
}
