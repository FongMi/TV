package com.fongmi.android.tv.ui.custom;

import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public abstract class FragmentStateManager {

    private final FragmentManager fm;
    private final ViewGroup container;

    public FragmentStateManager(ViewGroup container, FragmentManager fm) {
        this.container = container;
        this.fm = fm;
    }

    public abstract Fragment getItem(int position);

    public void change(int position) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentByTag(getTag(position));
        if (fragment == null) ft.add(container.getId(), fragment = getItem(position), getTag(position));
        else ft.show(fragment);
        Fragment current = fm.getPrimaryNavigationFragment();
        if (current != null) ft.hide(current);
        ft.setPrimaryNavigationFragment(fragment);
        ft.setReorderingAllowed(true);
        ft.commitNowAllowingStateLoss();
    }

    public String getTag(long id) {
        return "android:switcher:" + id;
    }

    public Fragment getFragment(long id) {
        return fm.findFragmentByTag(getTag(id));
    }
}
