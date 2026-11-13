package com.fongmi.android.tv.ui.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import com.fongmi.android.tv.ui.fragment.FolderFragment;

public abstract class CategoryPagerAdapter extends FragmentStatePagerAdapter {

    private final FragmentManager manager;

    protected CategoryPagerAdapter(FragmentManager manager) {
        super(manager);
        this.manager = manager;
    }

    public void clearPages(ViewPager pager) {
        pager.setAdapter(null);
        int containerId = pager.getId();
        FragmentTransaction transaction = manager.beginTransaction();
        for (Fragment fragment : manager.getFragments()) if (fragment instanceof FolderFragment && fragment.getId() == containerId) transaction.remove(fragment);
        transaction.commitNowAllowingStateLoss();
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
    }
}
