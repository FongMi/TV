package com.fongmi.bear.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.fongmi.bear.databinding.FragmentVodBinding;
import com.fongmi.bear.model.SiteViewModel;
import com.fongmi.bear.ui.adapter.VodAdapter;
import com.fongmi.bear.ui.custom.SpaceItemDecoration;

import java.util.HashMap;

public class VodFragment extends Fragment {

    private FragmentVodBinding mBinding;
    private SiteViewModel mSiteViewModel;
    private VodAdapter mVodAdapter;

    private String getTypeId() {
        return getArguments().getString("typeId");
    }

    public static VodFragment newInstance(String typeId) {
        Bundle args = new Bundle();
        args.putString("typeId", typeId);
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
}
