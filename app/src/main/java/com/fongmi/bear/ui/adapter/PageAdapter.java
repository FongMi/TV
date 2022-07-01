package com.fongmi.bear.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.fongmi.bear.bean.Result;
import com.fongmi.bear.ui.fragment.VodFragment;

public class PageAdapter extends FragmentStatePagerAdapter {

    private final Result result;

    public PageAdapter(@NonNull FragmentManager fm, Result result) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.result = result;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return VodFragment.newInstance(result.getTypes().get(position).getTypeId(), result.getFilters().get(result.getTypes().get(position).getTypeId()));
    }

    @Override
    public int getCount() {
        return result.getTypes().size();
    }
}