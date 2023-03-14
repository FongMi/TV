package com.fongmi.android.tv.ui.fragment.child;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentVodChildBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.fragment.VodFragment;

public class HomeFragment extends BaseFragment implements VodAdapter.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private FragmentVodChildBinding mBinding;
    private SiteViewModel mViewModel;
    private VodAdapter mAdapter;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    private VodFragment getParent() {
        return (VodFragment) getParentFragment();
    }

    private Site getSite() {
        return ApiConfig.get().getHome();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentVodChildBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setRecyclerView();
        setViewModel();
    }

    @Override
    protected void initEvent() {
        mBinding.swipeLayout.setOnRefreshListener(this);
    }

    @Override
    protected void initData() {
        mBinding.progressLayout.showProgress();
        getVideo();
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setLayoutManager(new GridLayoutManager(getContext(), Product.getColumn()));
        mBinding.recycler.setAdapter(mAdapter = new VodAdapter(this));
        mAdapter.setSize(Product.getSpec(getActivity()));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(getViewLifecycleOwner(), this::setAdapter);
    }

    private void getVideo() {
        if (!getSite().getKey().isEmpty()) mViewModel.homeContent();
    }

    private void setAdapter(Result result) {
        mBinding.swipeLayout.setRefreshing(false);
        mBinding.progressLayout.showContent();
        mAdapter.addAll(result.getList());
        getParent().setAdapter(result);
    }

    @Override
    public void onRefresh() {
        mAdapter.clear();
        getVideo();
    }

    @Override
    public void onItemClick(Vod item) {
        DetailActivity.start(getActivity(), item.getVodId(), item.getVodName());
    }

    @Override
    public boolean onLongClick(Vod item) {
        CollectActivity.start(getActivity(), item.getVodName());
        return true;
    }
}
