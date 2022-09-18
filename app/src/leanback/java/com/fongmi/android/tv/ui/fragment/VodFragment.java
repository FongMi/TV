package com.fongmi.android.tv.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.HorizontalGridView;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentVodBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.BaseFragment;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomScroller;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.presenter.FilterPresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class VodFragment extends BaseFragment implements CustomScroller.Callback, VodPresenter.OnClickListener {

    private HashMap<String, String> mExtend;
    private FragmentVodBinding mBinding;
    private ArrayObjectAdapter mAdapter;
    private ArrayObjectAdapter mLast;
    private SiteViewModel mViewModel;
    private CustomScroller mScroller;
    private List<Filter> mFilters;
    private List<String> mTypeIds;

    private String getTypeId() {
        return getArguments().getString("typeId");
    }

    private String getFilter() {
        return getArguments().getString("filter");
    }

    private boolean isFolder() {
        return getArguments().getBoolean("folder");
    }

    public static VodFragment newInstance(String typeId, String filter, boolean folder) {
        Bundle args = new Bundle();
        args.putString("typeId", typeId);
        args.putString("filter", filter);
        args.putBoolean("folder", folder);
        VodFragment fragment = new VodFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentVodBinding.inflate(inflater, container, false);
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

    @SuppressLint("RestrictedApi")
    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(8, FocusHighlight.ZOOM_FACTOR_NONE, HorizontalGridView.FOCUS_SCROLL_ALIGNED), FilterPresenter.class);
        mBinding.recycler.addOnScrollListener(mScroller = new CustomScroller(this));
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mBinding.recycler.setHeader(getActivity().findViewById(R.id.recycler));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(getViewLifecycleOwner(), result -> {
            mScroller.endLoading(result.getList().isEmpty());
            mBinding.progressLayout.showContent();
            addVideo(result.getList());
            checkPage();
        });
    }

    private void setClick(ArrayObjectAdapter adapter, String key, Filter.Value item) {
        if (mExtend.get(key) != null && Objects.equals(mExtend.get(key), item.getV())) return;
        for (int i = 0; i < adapter.size(); i++) ((Filter.Value) adapter.get(i)).setActivated(item);
        adapter.notifyArrayItemRangeChanged(0, adapter.size());
        mExtend.put(key, item.getV());
        getVideo();
    }

    private void getVideo() {
        mScroller.reset();
        getVideo(getTypeId(), "1");
    }

    private void checkPage() {
        if (mScroller.getPage() != 1 || mAdapter.size() >= 4 || isFolder()) return;
        mScroller.addPage();
        getVideo(getTypeId(), "2");
    }

    private void getVideo(String typeId, String page) {
        if (page.equals("1")) mLast = null;
        if (isFolder()) mTypeIds.add(typeId);
        if (isFolder()) mBinding.recycler.moveToTop();
        boolean clear = page.equals("1") && mAdapter.size() > mFilters.size();
        if (clear) mAdapter.removeItems(mFilters.size(), mAdapter.size() - mFilters.size());
        mViewModel.categoryContent(typeId, page, true, mExtend);
    }

    private boolean checkLastSize(List<Vod> items) {
        if (mLast == null || items.size() == 0) return false;
        int size = Prefers.getColumn() - mLast.size();
        if (size == 0) return false;
        size = Math.min(size, items.size());
        mLast.addAll(mLast.size(), new ArrayList<>(items.subList(0, size)));
        addVideo(new ArrayList<>(items.subList(size, items.size())));
        return true;
    }

    private void addVideo(List<Vod> items) {
        if (checkLastSize(items)) return;
        List<ListRow> rows = new ArrayList<>();
        for (List<Vod> part : Lists.partition(items, Prefers.getColumn())) {
            mLast = new ArrayObjectAdapter(new VodPresenter(this));
            mLast.setItems(part, null);
            rows.add(new ListRow(mLast));
        }
        mAdapter.addAll(mAdapter.size(), rows);
    }

    private void addFilter() {
        List<ListRow> rows = new ArrayList<>();
        for (Filter filter : mFilters) {
            FilterPresenter presenter = new FilterPresenter(filter.getKey());
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
            presenter.setOnClickListener((key, item) -> setClick(adapter, key, item));
            adapter.setItems(filter.getValue(), null);
            rows.add(new ListRow(adapter));
        }
        mAdapter.addAll(0, rows);
    }

    public void toggleFilter(boolean open) {
        if (open) {
            addFilter();
            mBinding.recycler.postDelayed(() -> mBinding.recycler.smoothScrollToPosition(0), 50);
        } else {
            mAdapter.removeItems(0, mFilters.size());
        }
    }

    public boolean canGoBack() {
        return mTypeIds.size() > 1;
    }

    public void goBack() {
        String typeId = mTypeIds.get(mTypeIds.size() - 2);
        mTypeIds = mTypeIds.subList(0, mTypeIds.size() - 2);
        getVideo(typeId, "1");
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.shouldSearch()) onLongClick(item);
        else if (item.getVodTag().equals("folder")) getVideo(item.getVodId(), "1");
        else DetailActivity.start(getActivity(), item.getVodId());
    }

    @Override
    public boolean onLongClick(Vod item) {
        CollectActivity.start(getActivity(), item.getVodName());
        return true;
    }

    @Override
    public void onLoadMore(String page) {
        if (isFolder()) return;
        mScroller.setLoading(true);
        getVideo(getTypeId(), page);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mBinding != null && !isVisibleToUser) mBinding.recycler.moveToTop();
    }
}
