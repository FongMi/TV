package com.fongmi.bear.ui.fragment;

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

import com.fongmi.bear.bean.Filter;
import com.fongmi.bear.bean.Vod;
import com.fongmi.bear.databinding.FragmentVodBinding;
import com.fongmi.bear.model.SiteViewModel;
import com.fongmi.bear.ui.custom.CustomRowPresenter;
import com.fongmi.bear.ui.custom.CustomSelector;
import com.fongmi.bear.ui.presenter.FilterPresenter;
import com.fongmi.bear.ui.presenter.VodPresenter;
import com.fongmi.bear.utils.ResUtil;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;

public class VodFragment extends Fragment {

    private HashMap<String, String> mExtend;
    private FragmentVodBinding mBinding;
    private SiteViewModel mSiteViewModel;
    private ArrayObjectAdapter mAdapter;
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
        getContent();
        setFilter();
    }

    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(8), FilterPresenter.class);
        ItemBridgeAdapter adapter = new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setAdapter(adapter);
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.mResult.observe(getViewLifecycleOwner(), result -> {
            for (List<Vod> items : Lists.partition(result.getList(), 5)) {
                ArrayObjectAdapter adapter = new ArrayObjectAdapter(new VodPresenter());
                adapter.addAll(0, items);
                mAdapter.add(new ListRow(adapter));
            }
        });
    }

    private void setFilter() {
        for (Filter filter : mFilters) {
            FilterPresenter presenter = new FilterPresenter(filter.getKey());
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
            adapter.addAll(0, filter.getValue());
            mAdapter.add(new ListRow(adapter));
            presenter.setOnClickListener((view, key, item) -> {
                if (mExtend.get(key) == null || !mExtend.get(key).equals(item.getV())) {
                    mExtend.put(key, item.getV());
                    getContent();
                }
            });
        }
    }

    private void getContent() {
        if (mAdapter.size() > mFilters.size()) mAdapter.removeItems(mFilters.size(), mAdapter.size() - mFilters.size());
        mSiteViewModel.categoryContent(getTypeId(), "1", true, mExtend);
    }
}
