package com.fongmi.bear.ui.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.fongmi.bear.bean.Result;
import com.fongmi.bear.ui.fragment.VodFragment;

public class PageAdapter extends FragmentStatePagerAdapter {

    private final Result mResult;

    public PageAdapter(@NonNull FragmentManager fm, Result result) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mResult = result;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mResult.getTypes().get(position).getTypeName();
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return VodFragment.newInstance(mResult.getTypes().get(position).getTypeId(), mResult.getFilters().get(mResult.getTypes().get(position).getTypeId()));
    }

    @Override
    public int getCount() {
        return mResult.getTypes().size();
    }
}