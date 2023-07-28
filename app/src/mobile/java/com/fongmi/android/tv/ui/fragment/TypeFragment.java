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
import com.fongmi.android.tv.bean.Page;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentTypeBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.CustomScroller;

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

    public static TypeFragment newInstance(String key, String typeId, boolean folder) {
        Bundle args = new Bundle();
        args.putString("key", key);
        args.putString("typeId", typeId);
        args.putBoolean("folder", folder);
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

    private boolean isFolder() {
        return getArguments().getBoolean("folder");
    }

    private boolean isHome() {
        return getTypeId().equals("home");
    }

    private VodFragment getParent() {
        return (VodFragment) getParentFragment();
    }

    private Page getLastPage() {
        return mPages.get(mPages.size() - 1);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentTypeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mScroller = new CustomScroller(this);
        mExtends = new HashMap<>();
        mPages = new ArrayList<>();
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
        mBinding.recycler.setAdapter(mAdapter = new VodAdapter(this));
        mAdapter.setSize(Product.getSpec(getActivity()));
        setViewType(isFolder());
    }

    private void setViewType(boolean folder) {
        mBinding.recycler.setLayoutManager(folder ? new LinearLayoutManager(getActivity()) : new GridLayoutManager(getContext(), Product.getColumn()));
        mAdapter.setViewType(folder ? ViewType.FOLDER : ViewType.GRID);
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
        int size = result.getList().size();
        mBinding.progressLayout.showContent(isFolder(), size);
        mBinding.swipeLayout.setRefreshing(false);
        mScroller.endLoading(size == 0);
        mAdapter.addAll(result.getList());
        addVideo(result.getList());
        checkPosition();
        checkPage(size);
    }

    private void addVideo(List<Vod> items) {
        if (items.isEmpty()) return;
        boolean folder = mPages.isEmpty() ? isFolder() : items.get(0).isList();
        int viewType = folder ? ViewType.FOLDER : ViewType.GRID;
        if (viewType != mAdapter.getViewType()) setViewType(folder);
        mAdapter.addAll(items);
    }

    private void checkPosition() {
        if (mPage != null) scrollToPosition(mPage.getPosition());
        else if (isFolder()) mBinding.recycler.scrollToPosition(0);
        mPage = null;
    }

    private void checkPage(int count) {
        if (count == 0 || mAdapter.getItemCount() >= 40 || isFolder() || isHome()) return;
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
        if (isFolder() || isHome()) return;
        mScroller.setLoading(true);
        getVideo(getTypeId(), page);
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isFolder()) {
            mPages.add(Page.get(item.getVodId(), findPosition()));
            getVideo(item.getVodId(), "1");
        } else {
            if (!isFolder()) DetailActivity.start(getActivity(), getKey(), item.getVodId(), item.getVodName());
            else DetailActivity.start(getActivity(), getKey(), item.getVodId(), item.getVodName(), item.getVodName());
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
