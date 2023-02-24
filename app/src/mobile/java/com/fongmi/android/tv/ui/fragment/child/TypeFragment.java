package com.fongmi.android.tv.ui.fragment.child;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentTypeBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.BaseFragment;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.custom.CustomScroller;
import com.fongmi.android.tv.ui.fragment.VodFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TypeFragment extends BaseFragment implements CustomScroller.Callback, VodAdapter.OnClickListener {

    private HashMap<String, String> mExtends;
    private FragmentTypeBinding mBinding;
    private CustomScroller mScroller;
    private SiteViewModel mViewModel;
    private VodAdapter mVodAdapter;
    private List<String> mTypeIds;

    public static TypeFragment newInstance(String typeId, boolean folder) {
        Bundle args = new Bundle();
        args.putString("typeId", typeId);
        args.putBoolean("folder", folder);
        TypeFragment fragment = new TypeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private VodFragment getParent() {
        return (VodFragment) getParentFragment();
    }

    private String getTypeId() {
        return getArguments().getString("typeId");
    }

    private boolean isFolder() {
        return getArguments().getBoolean("folder");
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentTypeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mTypeIds = new ArrayList<>();
        mExtends = new HashMap<>();
        mScroller = new CustomScroller(this);
        mBinding.progressLayout.showProgress();
        setRecyclerView();
        setViewModel();
        getVideo();
    }

    @Override
    protected void initEvent() {
        mBinding.recycler.addOnScrollListener(mScroller = new CustomScroller(this));
        mBinding.recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                getParent().toggleFilter(dy);
            }
        });
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setAdapter(mVodAdapter = new VodAdapter(this));
        mBinding.recycler.setLayoutManager(new GridLayoutManager(getContext(), Product.getColumn()));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(getViewLifecycleOwner(), result -> {
            mBinding.progressLayout.showContent(isFolder(), result.getList().size());
            mScroller.endLoading(result.getList().isEmpty());
            mVodAdapter.addAll(result.getList());
            checkPage();
        });
    }

    private void getVideo() {
        mScroller.reset();
        getVideo(getTypeId(), "1");
    }

    private void checkPage() {
        if (mScroller.getPage() != 1 || mVodAdapter.getItemCount() >= 4 || isFolder()) return;
        if (mScroller.addPage()) getVideo(getTypeId(), "2");
    }

    private void getVideo(String typeId, String page) {
        if (isFolder()) mTypeIds.add(typeId);
        if (isFolder()) mBinding.recycler.scrollToPosition(0);
        if (page.equals("1")) mVodAdapter.clear();
        if (page.equals("1")) mBinding.progressLayout.showProgress();
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
        if (item.shouldSearch()) onLongClick(item);
        else if (item.isFolder()) getVideo(item.getVodId(), "1");
        else DetailActivity.start(getActivity(), item.getVodId(), item.getVodName());
    }

    @Override
    public boolean onLongClick(Vod item) {
        CollectActivity.start(getActivity(), item.getVodName());
        return true;
    }

    @Override
    public boolean canBack() {
        if (mTypeIds.size() == 0) return true;
        refresh(2);
        return false;
    }
}
