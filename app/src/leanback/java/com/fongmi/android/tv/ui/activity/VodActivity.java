package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Cache;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.databinding.ActivityVodBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.adapter.CategoryPagerAdapter;
import com.fongmi.android.tv.ui.adapter.TypeAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.fragment.FolderFragment;
import com.fongmi.android.tv.utils.KeyUtil;
import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class VodActivity extends BaseActivity implements TypeAdapter.OnClickListener {

    private ActivityVodBinding mBinding;
    private TypeAdapter mAdapter;
    private View mOldView;
    private final Runnable mSelectPage = this::selectPage;

    public static void start(Activity activity, Result result) {
        start(activity, VodConfig.get().getHome().getKey(), result, true);
    }

    public static void start(Activity activity, String key, Result result) {
        start(activity, key, result, false);
    }

    private static void start(Activity activity, String key, Result result, boolean home) {
        if (result == null || result.getTypes().isEmpty()) return;
        Intent intent = new Intent(activity, VodActivity.class);
        intent.putExtra("key", key);
        intent.putExtra("result", result);
        intent.putExtra("home", home);
        activity.startActivity(intent);
    }

    private String getKey() {
        return getIntent().getStringExtra("key");
    }

    private Result getResult() {
        return getIntent().getParcelableExtra("result");
    }

    private Class getType() {
        return hasCategories() ? mAdapter.get(mBinding.pager.getCurrentItem()) : null;
    }

    private boolean hasCategories() {
        return mAdapter.getItemCount() > 0;
    }

    private FolderFragment getFragment() {
        return (FolderFragment) mBinding.pager.getAdapter().instantiateItem(mBinding.pager, mBinding.pager.getCurrentItem());
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityVodBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        setRecyclerView();
        setTypes();
        setPager();
        setViewModel();
    }

    @Override
    protected void initEvent() {
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                selectCategory(position);
            }
        });
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                onChildSelected(child);
            }
        });
    }

    private void setRecyclerView() {
        mBinding.recycler.requestFocus();
        mBinding.recycler.setHorizontalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.recycler.setAdapter(mAdapter = new TypeAdapter(this));
    }

    private void setTypes() {
        mAdapter.addAll(getResult().getTypes());
    }

    private void setPager() {
        mBinding.pager.setAdapter(new PageAdapter(getSupportFragmentManager()));
    }

    private void setViewModel() {
        SiteViewModel viewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        viewModel.getAction().observe(this, result -> WebActivity.handleAction(this, result));
    }

    private void applyHomeContent(Result result) {
        clearCategorySelection();
        ((PageAdapter) mBinding.pager.getAdapter()).clearPages(mBinding.pager);
        getIntent().putExtra("result", result);
        Cache.clear().put(result);
        setTypes();
        setPager();
        if (hasCategories()) selectCategory(0);
    }

    private void selectCategory(int position) {
        mBinding.recycler.setSelectedPosition(position);
        mBinding.recycler.requestFocus();
    }

    private void onChildSelected(@Nullable RecyclerView.ViewHolder child) {
        clearCategorySelection();
        if (child == null) return;
        mOldView = child.itemView;
        mOldView.setSelected(true);
        mBinding.recycler.postDelayed(mSelectPage, 100);
    }

    private void clearCategorySelection() {
        mBinding.recycler.removeCallbacks(mSelectPage);
        if (mOldView != null) mOldView.setSelected(false);
        mOldView = null;
    }

    private void selectPage() {
        int position = mBinding.recycler.getSelectedPosition();
        if (position >= 0 && position < mAdapter.getItemCount()) mBinding.pager.setCurrentItem(position);
    }

    private boolean isFilterVisible() {
        Class type = getType();
        return type != null && type.getFilter();
    }

    private void updateFilter(Class item) {
        int position = mAdapter.indexOf(item);
        if (!hasCategory(item, position)) return;
        item.setFilter(!item.getFilter());
        getFragment().toggleFilter(item.getFilter());
        mAdapter.notifyItemRangeChanged(position, 1);
    }

    private boolean hasCategory(Class item, int position) {
        return position >= 0 && mAdapter.get(position) == item;
    }

    public void closeFilter() {
        if (isFilterVisible()) updateFilter(getType());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.CATEGORY && hasCategories()) getFragment().onRefresh();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onHomeContentLoaded(RefreshEvent.HomeContentLoaded event) {
        if (getIntent().getBooleanExtra("home", false) && event.siteKey().equals(getKey())) applyHomeContent(event.result());
    }

    @Override
    protected void onDestroy() {
        if (mBinding != null) mBinding.recycler.removeCallbacks(mSelectPage);
        super.onDestroy();
    }

    @Override
    public void onItemClick(Class item) {
        updateFilter(item);
    }

    @Override
    public void onRefresh(Class item) {
        if (hasCategory(item, mAdapter.indexOf(item))) getFragment().onRefresh();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (KeyUtil.isMenuKey(event) && hasCategories()) updateFilter(getType());
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onBackInvoked() {
        if (!hasCategories()) super.onBackInvoked();
        else if (isFilterVisible()) closeFilter();
        else navigateCategoryBack();
    }

    private void navigateCategoryBack() {
        FolderFragment fragment = getFragment();
        if (fragment.moveToTop()) return;
        if (fragment.canBack()) fragment.goBack();
        else super.onBackInvoked();
    }

    class PageAdapter extends CategoryPagerAdapter {

        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Class type = mAdapter.get(position);
            return FolderFragment.newInstance(getKey(), type);
        }

        @Override
        public int getCount() {
            return mAdapter.getItemCount();
        }
    }
}
