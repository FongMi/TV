package com.fongmi.bear.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.fongmi.bear.bean.Type;
import com.fongmi.bear.ui.fragment.VodFragment;

import java.util.List;

public class PageAdapter extends FragmentStateAdapter {

    private final List<Type> mItems;

    public PageAdapter(@NonNull FragmentActivity activity, List<Type> items) {
        super(activity);
        mItems = items;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return VodFragment.newInstance(mItems.get(position).getTypeId());
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }
}