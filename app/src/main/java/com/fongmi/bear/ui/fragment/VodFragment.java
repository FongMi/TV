package com.fongmi.bear.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fongmi.bear.bean.Filter;
import com.fongmi.bear.databinding.FragmentVodBinding;
import com.fongmi.bear.databinding.ViewFilterBinding;
import com.fongmi.bear.model.SiteViewModel;
import com.fongmi.bear.ui.adapter.FilterAdapter;
import com.fongmi.bear.ui.adapter.VodAdapter;
import com.fongmi.bear.ui.custom.SpaceItemDecoration;
import com.fongmi.bear.utils.ResUtil;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;

public class VodFragment extends Fragment {

    private FragmentVodBinding mBinding;
    private SiteViewModel mSiteViewModel;
    private VodAdapter mVodAdapter;

    private String getTypeId() {
        return getArguments().getString("typeId");
    }

    private String getFilter() {
        return getArguments().getString("filter");
    }

    public static VodFragment newInstance(String typeId, List<Filter> filters) {
        Bundle args = new Bundle();
        args.putString("typeId", typeId);
        args.putString("filter", new Gson().toJson(filters));
        VodFragment fragment = new VodFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentVodBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setRecyclerView();
        setViewModel();
        setFilters();
        getContent();
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setLayoutManager(new GridLayoutManager(getContext(), 5));
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(5, 12, false, 0));
        mBinding.recycler.setAdapter(mVodAdapter = new VodAdapter());
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.mResult.observe(getViewLifecycleOwner(), result -> {
            mVodAdapter.addAll(result.getList());
            mBinding.progress.showContent();
        });
    }

    private void getContent() {
        mBinding.progress.showProgress();
        mSiteViewModel.categoryContent(getTypeId(), "1", true, new HashMap<>());
    }

    private void setFilters() {
        List<Filter> filters = Filter.arrayFrom(getFilter());
        for (Filter filter : filters) {
            ViewFilterBinding binding = ViewFilterBinding.inflate(getLayoutInflater());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = ResUtil.dp2px(12);
            FilterAdapter adapter = new FilterAdapter(filter.getValue());
            adapter.setOnItemClickListener(new FilterAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {

                }
            });
            binding.name.setText(filter.getName());
            binding.recycler.setHasFixedSize(true);
            binding.recycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            binding.recycler.setAdapter(adapter);
            mBinding.filter.addView(binding.getRoot(), params);
        }
    }
}
