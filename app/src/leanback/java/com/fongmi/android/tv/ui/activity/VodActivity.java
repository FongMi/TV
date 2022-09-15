package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.databinding.ActivityVodBinding;
import com.fongmi.android.tv.ui.fragment.VodFragment;
import com.fongmi.android.tv.ui.presenter.TypePresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VodActivity extends BaseActivity {

    private ActivityVodBinding mBinding;
    private ArrayObjectAdapter mAdapter;
    private PageAdapter mPageAdapter;
    private Handler mHandler;
    private Result mResult;
    private View mOldView;

    private String getResult() {
        return getIntent().getStringExtra("result");
    }

    public static void start(Activity activity, Result result) {
        if (result == null || result.getTypes().isEmpty()) return;
        Intent intent = new Intent(activity, VodActivity.class);
        result.setList(Collections.emptyList());
        intent.putExtra("result", result.toString());
        activity.startActivity(intent);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityVodBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mHandler = new Handler(Looper.getMainLooper());
        mResult = Result.fromJson(getResult());
        setRecyclerView();
        setTypes();
        setPager();
    }

    @Override
    protected void initEvent() {
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBinding.recycler.setSelectedPosition(position);
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
        mBinding.recycler.setHorizontalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(new TypePresenter(this::updateFilter))));
    }

    private List<Class> getTypes() {
        List<Class> types = new ArrayList<>();
        for (String cate : ApiConfig.get().getHome().getCategories()) for (Class type : mResult.getTypes()) if (cate.equals(type.getTypeName())) types.add(type);
        return types;
    }

    private void setTypes() {
        mResult.setTypes(getTypes());
        Boolean filter = ApiConfig.get().getHome().isFilterable() ? false : null;
        for (Class item : mResult.getTypes()) if (mResult.getFilters().containsKey(item.getTypeId())) item.setFilter(filter);
        mAdapter.setItems(mResult.getTypes(), null);
    }

    private void setPager() {
        mBinding.pager.setAdapter(mPageAdapter = new PageAdapter(getSupportFragmentManager()));
    }

    private void onChildSelected(@Nullable RecyclerView.ViewHolder child) {
        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, 200);
        if (mOldView != null) mOldView.setActivated(false);
        if (child == null) return;
        mOldView = child.itemView;
        mOldView.setActivated(true);
    }

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mBinding.pager.setCurrentItem(mBinding.recycler.getSelectedPosition());
        }
    };

    private void updateFilter(Class item) {
        if (item.getFilter() == null) return;
        getFragment().toggleFilter(item.toggleFilter().getFilter());
        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean isMenuUp = event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_MENU;
        if (isMenuUp) updateFilter(mResult.getTypes().get(mBinding.pager.getCurrentItem()));
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        Class item = mResult.getTypes().get(mBinding.pager.getCurrentItem());
        if (item.getFilter() != null && item.getFilter()) updateFilter(item);
        else if (getFragment().canGoBack()) getFragment().goBack();
        else super.onBackPressed();
    }

    private VodFragment getFragment() {
        return (VodFragment) mPageAdapter.instantiateItem(mBinding.pager, mBinding.pager.getCurrentItem());
    }

    class PageAdapter extends FragmentStatePagerAdapter {

        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Class type = mResult.getTypes().get(position);
            String filter = new Gson().toJson(mResult.getFilters().get(type.getTypeId()));
            return VodFragment.newInstance(type.getTypeId(), filter, type.getTypeFlag().equals("1"));
        }

        @Override
        public int getCount() {
            return mResult.getTypes().size();
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        }
    }
}
