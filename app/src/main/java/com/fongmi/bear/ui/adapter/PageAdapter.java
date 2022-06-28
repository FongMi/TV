package com.fongmi.bear.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.fongmi.bear.bean.Class;
import com.fongmi.bear.bean.Filter;
import com.fongmi.bear.ui.fragment.VodFragment;

import java.util.LinkedHashMap;
import java.util.List;

public class PageAdapter extends FragmentStateAdapter {

    private final List<Class> mItems;
    private final LinkedHashMap<String, List<Filter>> mFilters;

    public PageAdapter(@NonNull FragmentActivity activity, List<Class> items, LinkedHashMap<String, List<Filter>> filters) {
        super(activity);
        mItems = items;
        mFilters = filters;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return VodFragment.newInstance(mItems.get(position).getTypeId(), mFilters.get(mItems.get(position).getTypeId()));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}