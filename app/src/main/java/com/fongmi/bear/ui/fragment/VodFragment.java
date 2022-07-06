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

import com.fongmi.bear.R;
import com.fongmi.bear.bean.Filter;
import com.fongmi.bear.bean.Vod;
import com.fongmi.bear.databinding.FragmentVodBinding;
import com.fongmi.bear.model.SiteViewModel;
import com.fongmi.bear.ui.activity.DetailActivity;
import com.fongmi.bear.ui.custom.CustomRowPresenter;
import com.fongmi.bear.ui.custom.CustomSelector;
import com.fongmi.bear.ui.custom.Scroller;
import com.fongmi.bear.ui.presenter.FilterPresenter;
import com.fongmi.bear.ui.presenter.ProgressPresenter;
import com.fongmi.bear.ui.presenter.VodPresenter;
import com.fongmi.bear.utils.ResUtil;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class VodFragment extends Fragment implements Scroller.Callback, VodPresenter.OnClickListener {

    private HashMap<String, String> mExtend;
    private FragmentVodBinding mBinding;
    private SiteViewModel mSiteViewModel;
    private ArrayObjectAdapter mAdapter;
    private List<Filter> mFilters;
    private Scroller mScroller;

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
        setFilter();
        getVideo();
    }

    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(String.class, new ProgressPresenter());
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(8), FilterPresenter.class);
        mBinding.recycler.addOnScrollListener(mScroller = new Scroller(this));
        mBinding.recycler.setTabView(getActivity().findViewById(R.id.recycler));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.mResult.observe(getViewLifecycleOwner(), result -> {
            mAdapter.remove("progress");
            mScroller.endLoading(result.getList().isEmpty());
            for (List<Vod> items : result.partition()) {
                VodPresenter presenter = new VodPresenter(items.size());
                ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
                presenter.setOnClickListener(this);
                adapter.addAll(0, items);
                mAdapter.add(new ListRow(adapter));
            }
        });
    }

    private void setFilter() {
        for (Filter filter : mFilters) {
            FilterPresenter presenter = new FilterPresenter(filter.getKey());
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenter);
            presenter.setOnClickListener((key, item) -> setClick(adapter, key, item));
            adapter.addAll(0, filter.getValue());
            mAdapter.add(new ListRow(adapter));
        }
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

    private void getVideo(String page) {
        boolean clear = page.equals("1") && mAdapter.size() > mFilters.size();
        if (clear) mAdapter.removeItems(mFilters.size(), mAdapter.size() - mFilters.size());
        mSiteViewModel.categoryContent(getTypeId(), page, true, mExtend);
        mAdapter.add("progress");
    }

    @Override
    public void onItemClick(Vod item) {
        DetailActivity.start(getActivity(), item.getVodId());
    }

    @Override
    public void onLoadMore(String page) {
        getVideo(page);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mBinding != null && !isVisibleToUser) mBinding.recycler.moveToTop();
    }
}
