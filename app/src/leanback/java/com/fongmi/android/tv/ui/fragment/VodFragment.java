package com.fongmi.android.tv.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.lifecycle.ViewModelProvider;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentVodBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomScroller;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.presenter.FilterPresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class VodFragment extends Fragment implements CustomScroller.Callback, VodPresenter.OnClickListener {

    private HashMap<String, String> mExtend;
    private FragmentVodBinding mBinding;
    private SiteViewModel mSiteViewModel;
    private ArrayObjectAdapter mAdapter;
    private ArrayObjectAdapter mLast;
    private CustomScroller mScroller;
    private List<Filter> mFilters;

    private String getTypeId() {
        return getArguments().getString("typeId");
    }

    private String getFilter() {
        return getArguments().getString("filter");
    }

    public static VodFragment newInstance(String typeId, List<Filter> filter) {
        Bundle args = new Bundle();
        args.putString("typeId", typeId);
        args.putString("filter", new Gson().toJson(filter));
        VodFragment fragment = new VodFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentVodBinding.inflate(inflater, container, false);
        mFilters = Filter.arrayFrom(getFilter());
        mExtend = new HashMap<>();
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setRecyclerView();
        setViewModel();
        getVideo();
    }

    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(8), FilterPresenter.class);
        mBinding.recycler.addOnScrollListener(mScroller = new CustomScroller(this));
        mBinding.recycler.setTabView(getActivity().findViewById(R.id.recycler));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.result.observe(getViewLifecycleOwner(), result -> {
            mScroller.endLoading(result.getList().isEmpty());
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
        getVideo("1");
    }

    private void checkPage() {
        if (mScroller.getPage() != 1 || mAdapter.size() >= 4) return;
        mScroller.addPage();
        getVideo("2");
    }

    private void getVideo(String page) {
        boolean clear = page.equals("1") && mAdapter.size() > mFilters.size();
        if (clear) mAdapter.removeItems(mFilters.size(), mAdapter.size() - mFilters.size());
        mSiteViewModel.categoryContent(getTypeId(), page, true, mExtend);
    }

    private boolean checkLastSize(List<Vod> items) {
        if (mLast == null || items.size() == 0) return false;
        int size = 5 - mLast.size();
        if (size == 0) return false;
        mLast.addAll(mLast.size(), new ArrayList<>(items.subList(0, size)));
        addVideo(new ArrayList<>(items.subList(size, items.size())));
        return true;
    }

    private void addVideo(List<Vod> items) {
        if (checkLastSize(items)) return;
        List<ListRow> rows = new ArrayList<>();
        for (List<Vod> part : Lists.partition(items, 5)) {
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

    @Override
    public void onItemClick(Vod item) {
        DetailActivity.start(getActivity(), item.getVodId());
    }

    @Override
    public void onLoadMore(String page) {
        mScroller.setLoading(true);
        getVideo(page);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mBinding != null && !isVisibleToUser) mBinding.recycler.moveToTop();
    }
}
