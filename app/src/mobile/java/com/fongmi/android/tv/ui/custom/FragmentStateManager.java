package com.fongmi.android.tv.ui.custom;

import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fongmi.android.tv.ui.activity.BaseFragment;

public abstract class FragmentStateManager {

    private final FragmentManager fm;
    private final ViewGroup container;

    public FragmentStateManager(ViewGroup container, FragmentManager fm) {
        this.container = container;
        this.fm = fm;
    }

    public abstract Fragment getItem(int position);

    public boolean change(int position) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentByTag(getTag(position));
        if (fragment == null) ft.add(container.getId(), fragment = getItem(position), getTag(position));
        else ft.show(fragment);
        Fragment current = fm.getPrimaryNavigationFragment();
        if (current != null) ft.hide(current);
        ft.setPrimaryNavigationFragment(fragment);
        ft.setReorderingAllowed(true);
        ft.commitNowAllowingStateLoss();
        return true;
    }

    private String getTag(int position) {
        return "android:switcher:" + position;
    }

    public BaseFragment getFragment(int position) {
        return (BaseFragment) fm.findFragmentByTag(getTag(position));
    }

    public boolean isVisible(int position) {
        Fragment fragment = getFragment(position);
        return fragment != null && fragment.isVisible();
    }

    public boolean canBack(int position) {
        BaseFragment fragment = getFragment(position);
        return fragment != null && fragment.canBack();
    }
}
