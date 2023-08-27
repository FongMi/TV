package com.fongmi.android.tv.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Page;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentTypeBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.CustomScroller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TypeFragment extends BaseFragment implements CustomScroller.Callback, VodAdapter.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    private HashMap<String, String> mExtends;
    private FragmentTypeBinding mBinding;
    private CustomScroller mScroller;
    private SiteViewModel mViewModel;
    private VodAdapter mAdapter;
    private List<Page> mPages;
    private Page mPage;

    public static TypeFragment newInstance(String key, String typeId, HashMap<String, String> extend, boolean folder) {
        Bundle args = new Bundle();
        args.putString("key", key);
        args.putString("typeId", typeId);
        args.putBoolean("folder", folder);
        args.putSerializable("extend", extend);
        TypeFragment fragment = new TypeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private String getKey() {
        return getArguments().getString("key");
    }

    private String getTypeId() {
        return mPages.isEmpty() ? getArguments().getString("typeId") : getLastPage().getVodId();
    }

    private HashMap<String, String> getExtend() {
        Serializable extend = getArguments().getSerializable("extend");
        return extend == null ? new HashMap<>() : (HashMap<String, String>) extend;
    }

    private boolean isFolder() {
        return getArguments().getBoolean("folder");
    }

    private boolean isHome() {
        return getTypeId().equals("home");
    }

    private Site getSite() {
        return ApiConfig.get().getSite(getKey());
    }

    private VodFragment getParent() {
        return (VodFragment) getParentFragment();
    }

    private Page getLastPage() {
        return mPages.get(mPages.size() - 1);
    }

    private Style getStyle() {
        return isFolder() ? Style.list() : getSite().getStyle();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentTypeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mScroller = new CustomScroller(this);
        mPages = new ArrayList<>();
        mExtends = getExtend();
        setRecyclerView();
        setViewModel();
    }

    @Override
    protected void initEvent() {
        mBinding.swipeLayout.setEnabled(!isHome());
        mBinding.swipeLayout.setOnRefreshListener(this);
        mBinding.recycler.addOnScrollListener(mScroller = new CustomScroller(this));
    }

    @Override
    protected void initData() {
        mBinding.progressLayout.showProgress();
        getVideo();
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        setStyle(getStyle());
    }

    private void setStyle(Style style) {
        mBinding.recycler.setAdapter(mAdapter = new VodAdapter(this, style, Product.getSpec(getActivity(), style)));
        mBinding.recycler.setLayoutManager(style.isList() ? new LinearLayoutManager(getActivity()) : new GridLayoutManager(getContext(), Product.getColumn(style)));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(getViewLifecycleOwner(), this::setAdapter);
    }

    private void getVideo() {
        mScroller.reset();
        getVideo(getTypeId(), "1");
    }

    private void getVideo(String typeId, String page) {
        if (page.equals("1")) mAdapter.clear();
        if (page.equals("1") && !mBinding.swipeLayout.isRefreshing()) mBinding.progressLayout.showProgress();
        if (isHome() && page.equals("1")) setAdapter(getParent().getResult());
        else mViewModel.categoryContent(getKey(), typeId, page, true, mExtends);
    }

    private void setAdapter(Result result) {
        boolean first = mScroller.first();
        int size = result.getList().size();
        mBinding.progressLayout.showContent(first, size);
        mBinding.swipeLayout.setRefreshing(false);
        if (size > 0) addVideo(result);
        mScroller.endLoading(result);
        checkPosition(first);
        checkMore(size);
    }

    private void addVideo(Result result) {
        Style style = result.getList().get(0).getStyle(getStyle());
        if (!style.equals(mAdapter.getStyle())) setStyle(style);
        mAdapter.addAll(result.getList());
    }

    private void checkPosition(boolean first) {
        if (mPage != null) scrollToPosition(mPage.getPosition());
        else if (first) mBinding.recycler.scrollToPosition(0);
        mPage = null;
    }

    private void checkMore(int count) {
        if (mScroller.isDisable() || count == 0 || mAdapter.getItemCount() >= 40 || isHome()) return;
        getVideo(getTypeId(), String.valueOf(mScroller.addPage()));
    }

    private int findPosition() {
        if (mBinding.recycler.getLayoutManager() instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) mBinding.recycler.getLayoutManager()).findFirstVisibleItemPosition();
        } else if (mBinding.recycler.getLayoutManager() instanceof GridLayoutManager) {
            return ((GridLayoutManager) mBinding.recycler.getLayoutManager()).findFirstVisibleItemPosition();
        } else {
            return 0;
        }
    }

    private void scrollToPosition(int position) {
        if (mBinding.recycler.getLayoutManager() instanceof LinearLayoutManager) {
            ((LinearLayoutManager) mBinding.recycler.getLayoutManager()).scrollToPositionWithOffset(position, 0);
        } else if (mBinding.recycler.getLayoutManager() instanceof GridLayoutManager) {
            ((GridLayoutManager) mBinding.recycler.getLayoutManager()).scrollToPositionWithOffset(position, 0);
        }
    }

    public void setFilter(String key, String value) {
        mExtends.put(key, value);
        onRefresh();
    }

    @Override
    public void onRefresh() {
        getVideo();
    }

    @Override
    public void onLoadMore(String page) {
        if (isHome()) return;
        mScroller.setLoading(true);
        getVideo(getTypeId(), page);
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isFolder()) {
            mPages.add(Page.get(item.getVodId(), findPosition()));
            getVideo(item.getVodId(), "1");
        } else {
            if (!isFolder()) DetailActivity.start(getActivity(), getKey(), item.getVodId(), item.getVodName(), item.getVodPic());
            else DetailActivity.start(getActivity(), getKey(), item.getVodId(), item.getVodName(), item.getVodPic(), item.getVodName());
        }
    }

    @Override
    public boolean onLongClick(Vod item) {
        CollectActivity.start(getActivity(), item.getVodName());
        return true;
    }

    @Override
    public boolean canBack() {
        if (mPages.isEmpty()) return true;
        mPages.remove(mPage = getLastPage());
        onRefresh();
        return false;
    }
}
