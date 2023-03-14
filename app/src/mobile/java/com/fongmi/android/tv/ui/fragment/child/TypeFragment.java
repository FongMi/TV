package com.fongmi.android.tv.ui.fragment.child;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentVodChildBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.CustomScroller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TypeFragment extends BaseFragment implements CustomScroller.Callback, VodAdapter.OnClickListener {

    private HashMap<String, String> mExtends;
    private FragmentVodChildBinding mBinding;
    private CustomScroller mScroller;
    private SiteViewModel mViewModel;
    private List<String> mTypeIds;
    private VodAdapter mAdapter;

    public static TypeFragment newInstance(String typeId, boolean folder) {
        Bundle args = new Bundle();
        args.putString("typeId", typeId);
        args.putBoolean("folder", folder);
        TypeFragment fragment = new TypeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private String getTypeId() {
        return getArguments().getString("typeId");
    }

    private boolean isFolder() {
        return getArguments().getBoolean("folder");
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentVodChildBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mTypeIds = new ArrayList<>();
        mExtends = new HashMap<>();
        mScroller = new CustomScroller(this);
        setRecyclerView();
        setViewModel();
    }

    @Override
    protected void initEvent() {
        mBinding.swipeLayout.setOnRefreshListener(this::getVideo);
        mBinding.recycler.addOnScrollListener(mScroller = new CustomScroller(this));
    }

    @Override
    protected void initData() {
        mBinding.progressLayout.showProgress();
        getVideo();
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setAdapter(mAdapter = new VodAdapter(this));
        mBinding.recycler.setLayoutManager(new GridLayoutManager(getContext(), Product.getColumn()));
        mAdapter.setSize(Product.getSpec(getActivity()));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(getViewLifecycleOwner(), this::setAdapter);
    }

    private void getVideo() {
        mTypeIds.clear();
        mScroller.reset();
        getVideo(getTypeId(), "1");
    }

    private void setAdapter(Result result) {
        int size = result.getList().size();
        mBinding.progressLayout.showContent(isFolder(), size);
        mBinding.swipeLayout.setRefreshing(false);
        mScroller.endLoading(size == 0);
        mAdapter.addAll(result.getList());
        checkPage(size);
    }

    private void checkPage(int count) {
        if (count == 0 || mAdapter.getItemCount() >= 40 || isFolder()) return;
        getVideo(getTypeId(), String.valueOf(mScroller.addPage()));
    }

    private void getVideo(String typeId, String page) {
        if (isFolder()) mTypeIds.add(typeId);
        if (isFolder()) mBinding.recycler.scrollToPosition(0);
        if (page.equals("1")) mAdapter.clear();
        if (page.equals("1") && !mBinding.swipeLayout.isRefreshing()) mBinding.progressLayout.showProgress();
        mViewModel.categoryContent(ApiConfig.get().getHome().getKey(), typeId, page, true, mExtends);
    }

    private void refresh(int num) {
        String typeId = mTypeIds.get(mTypeIds.size() - num);
        mTypeIds = mTypeIds.subList(0, mTypeIds.size() - num);
        getVideo(typeId, "1");
    }

    @Override
    public void onLoadMore(String page) {
        if (isFolder()) return;
        mScroller.setLoading(true);
        getVideo(getTypeId(), page);
    }

    public void setFilter(String key, String value) {
        mExtends.put(key, value);
        if (isFolder()) refresh(1);
        else getVideo();
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isFolder()) getVideo(item.getVodId(), "1");
        else DetailActivity.start(getActivity(), item.getVodId(), item.getVodName());
    }

    @Override
    public boolean onLongClick(Vod item) {
        CollectActivity.start(getActivity(), item.getVodName());
        return true;
    }

    @Override
    public boolean canBack() {
        if (mTypeIds.size() < 2) return true;
        refresh(2);
        return false;
    }
}
