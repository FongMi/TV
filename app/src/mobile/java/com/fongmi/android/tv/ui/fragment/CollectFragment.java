package com.fongmi.android.tv.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Collect;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentCollectBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.FolderActivity;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.ui.adapter.CollectAdapter;
import com.fongmi.android.tv.ui.adapter.SearchAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.CustomScroller;
import com.fongmi.android.tv.utils.PauseExecutor;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class CollectFragment extends BaseFragment implements MenuProvider, CollectAdapter.OnClickListener, SearchAdapter.OnClickListener, CustomScroller.Callback {

    private FragmentCollectBinding mBinding;
    private CollectAdapter mCollectAdapter;
    private SearchAdapter mSearchAdapter;
    private CustomScroller mScroller;
    private SiteViewModel mViewModel;
    private PauseExecutor mExecutor;
    private int maxWidth;

    public static CollectFragment newInstance(String keyword) {
        Bundle args = new Bundle();
        args.putString("keyword", keyword);
        CollectFragment fragment = new CollectFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private String getKeyword() {
        return getArguments().getString("keyword");
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentCollectBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initMenu() {
        if (isHidden()) return;
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(mBinding.toolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.addMenuProvider(this, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
        activity.setTitle(getKeyword());
    }

    @Override
    protected void initView() {
        maxWidth = ResUtil.getScreenWidth() / (ResUtil.isLand(requireActivity()) ? 3 : 2) - ResUtil.dp2px(32);
        mScroller = new CustomScroller(this);
        setRecyclerView();
        setViewModel();
        search();
    }

    private void setRecyclerView() {
        mBinding.collect.setMaxWidth(maxWidth);
        mBinding.collect.setItemAnimator(null);
        mBinding.collect.setHasFixedSize(false);
        mBinding.collect.setAdapter(mCollectAdapter = new CollectAdapter(this));
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.addOnScrollListener(mScroller);
        mBinding.recycler.setAdapter(mSearchAdapter = new SearchAdapter(this));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.search.observe(this, result -> {
            if (mCollectAdapter.getPosition() == 0) mSearchAdapter.addItems(result.getList());
            mCollectAdapter.addItem(Collect.create(result.getList()));
            mCollectAdapter.add(result.getList());
        });
        mViewModel.result.observe(this, result -> {
            boolean same = !result.getList().isEmpty() && mCollectAdapter.getActivated().getSite().equals(result.getList().get(0).getSite());
            if (same) mCollectAdapter.getActivated().getList().addAll(result.getList());
            if (same) mSearchAdapter.addItems(result.getList());
            mScroller.endLoading(result);
        });
    }

    private List<Site> getSites() {
        List<Site> items = new ArrayList<>();
        for (Site site : VodConfig.get().getSites()) if (site.isSearchable()) items.add(site);
        return items;
    }

    private void search() {
        mExecutor = new PauseExecutor(20);
        mCollectAdapter.setItems(List.of(Collect.all()));
        for (Site site : getSites()) mExecutor.execute(() -> search(site, getKeyword()));
    }

    private void search(Site site, String keyword) {
        try {
            mViewModel.searchContent(site, keyword, false);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onItemClick(int position, Collect item) {
        mSearchAdapter.setItems(item.getList(), () -> mBinding.recycler.scrollToPosition(0));
        mCollectAdapter.setActivated(position);
        mScroller.setPage(item.getPage());
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.isFolder()) FolderActivity.start(requireActivity(), item.getSiteKey(), Result.folder(item));
        else VideoActivity.collect(requireActivity(), item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onLoadMore(String page) {
        Collect activated = mCollectAdapter.getActivated();
        if ("all".equals(activated.getSite().getKey())) return;
        mViewModel.searchContent(activated.getSite(), getKeyword(), page);
        activated.setPage(Integer.parseInt(page));
        mScroller.setLoading(true);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) requireActivity().getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mExecutor != null) mExecutor.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mExecutor != null) mExecutor.pause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) requireActivity().removeMenuProvider(this);
        else initMenu();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().removeMenuProvider(this);
        if (mExecutor != null) mExecutor.shutdownNow();
    }
}
