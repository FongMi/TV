package com.fongmi.android.tv.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentChildBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.BaseFragment;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.custom.CustomScroller;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChildFragment extends BaseFragment implements CustomScroller.Callback, VodAdapter.OnClickListener {

    private FragmentChildBinding mBinding;
    private HashMap<String, String> mExtend;
    private SiteViewModel mViewModel;
    private CustomScroller mScroller;
    private List<Filter> mFilters;
    private List<String> mTypeIds;
    private VodAdapter mAdapter;

    private String getTypeId() {
        return getArguments().getString("typeId");
    }

    private String getFilter() {
        return getArguments().getString("filter");
    }

    private boolean isFolder() {
        return getArguments().getBoolean("folder");
    }

    public static ChildFragment newInstance(String typeId, String filter, boolean folder) {
        Bundle args = new Bundle();
        args.putString("typeId", typeId);
        args.putString("filter", filter);
        args.putBoolean("folder", folder);
        ChildFragment fragment = new ChildFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentChildBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mTypeIds = new ArrayList<>();
        mExtend = new HashMap<>();
        mFilters = Filter.arrayFrom(getFilter());
        mBinding.progressLayout.showProgress();
        setRecyclerView();
        setViewModel();
        getVideo();
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setAdapter(mAdapter = new VodAdapter(this));
        mBinding.recycler.addOnScrollListener(mScroller = new CustomScroller(this));
        mBinding.recycler.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(3, 16));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(getViewLifecycleOwner(), result -> {
            mScroller.endLoading(result.getList().isEmpty());
            mBinding.progressLayout.showContent();
            mAdapter.addAll(result.getList());
            checkPage();
        });
    }

    private void getVideo() {
        mScroller.reset();
        getVideo(getTypeId(), "1");
    }

    private void checkPage() {
        if (mScroller.getPage() != 1 || mAdapter.getItemCount() >= 4 || isFolder()) return;
        mScroller.addPage();
        getVideo(getTypeId(), "2");
    }

    private void getVideo(String typeId, String page) {
        if (isFolder()) mTypeIds.add(typeId);
        if (isFolder()) mBinding.recycler.scrollToPosition(0);
        boolean clear = page.equals("1") && mAdapter.getItemCount() > mFilters.size();
        //if (clear) mAdapter.removeItems(mFilters.size(), mAdapter.size() - mFilters.size());
        //mViewModel.categoryContent(typeId, page, true, mExtend);
    }

    @Override
    public void onLoadMore(String page) {
        if (isFolder()) return;
        mScroller.setLoading(true);
        getVideo(getTypeId(), page);
    }

    @Override
    public void onItemClick(Vod item) {

    }

    @Override
    public boolean onLongClick(Vod item) {
        return false;
    }
}
