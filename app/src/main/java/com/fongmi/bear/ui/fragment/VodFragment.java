package com.fongmi.bear.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fongmi.bear.bean.Filter;
import com.fongmi.bear.databinding.FragmentVodBinding;
import com.fongmi.bear.databinding.ViewFilterBinding;
import com.fongmi.bear.model.SiteViewModel;
import com.fongmi.bear.ui.adapter.FilterAdapter;
import com.fongmi.bear.ui.adapter.VodAdapter;
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
        mBinding.recycler.setNumColumns(5);
        mBinding.recycler.setItemSpacing(ResUtil.dp2px(12));
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

    private void setFilter() {
        for (Filter filter : Filter.arrayFrom(getFilter())) {
            ViewFilterBinding binding = ViewFilterBinding.inflate(getLayoutInflater());
            FilterAdapter adapter = new FilterAdapter(filter.getValue());
            adapter.setOnItemClickListener(position -> {

            });
            binding.name.setText(filter.getName());
            binding.recycler.setAdapter(adapter);
            binding.recycler.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            mBinding.filter.addView(binding.getRoot());
        }
    }
}
