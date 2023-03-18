package com.fongmi.android.tv.ui.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Hot;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.FragmentVodBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.FilterCallback;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.HistoryActivity;
import com.fongmi.android.tv.ui.activity.KeepActivity;
import com.fongmi.android.tv.ui.adapter.TypeAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.dialog.FilterDialog;
import com.fongmi.android.tv.ui.custom.dialog.LinkDialog;
import com.fongmi.android.tv.ui.custom.dialog.SiteDialog;
import com.fongmi.android.tv.ui.fragment.child.HomeFragment;
import com.fongmi.android.tv.ui.fragment.child.TypeFragment;
import com.fongmi.android.tv.utils.Prefers;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Response;

public class VodFragment extends BaseFragment implements SiteCallback, FilterCallback, TypeAdapter.OnClickListener {

    private FragmentVodBinding mBinding;
    private TypeAdapter mAdapter;
    private Runnable mRunnable;
    private List<String> mHots;

    public static VodFragment newInstance() {
        return new VodFragment();
    }

    private BaseFragment getFragment() {
        return (BaseFragment) mBinding.pager.getAdapter().instantiateItem(mBinding.pager, mBinding.pager.getCurrentItem());
    }

    private Site getSite() {
        return ApiConfig.get().getHome();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentVodBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        setRecyclerView();
        initHot();
        getHot();
    }

    @Override
    protected void initEvent() {
        mBinding.hot.setOnClickListener(this::onHot);
        mBinding.link.setOnClickListener(this::onLink);
        mBinding.logo.setOnClickListener(this::onLogo);
        mBinding.keep.setOnClickListener(this::onKeep);
        mBinding.filter.setOnClickListener(this::onFilter);
        mBinding.search.setOnClickListener(this::onSearch);
        mBinding.history.setOnClickListener(this::onHistory);
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBinding.type.smoothScrollToPosition(position);
                mAdapter.setActivated(position);
                setFabVisible(position);
            }
        });
    }

    private void setRecyclerView() {
        mBinding.type.setHasFixedSize(true);
        mBinding.type.setItemAnimator(null);
        mBinding.type.setAdapter(mAdapter = new TypeAdapter(this));
    }

    private void initHot() {
        mHots = Hot.get(Prefers.getHot());
        App.post(mRunnable = this::updateHot, 0);
    }

    private void getHot() {
        OkHttp.newCall("https://api.web.360kan.com/v1/rank?cat=1").enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                mHots = Hot.get(response.body().string());
            }
        });
    }

    private void updateHot() {
        App.post(mRunnable, 10 * 1000);
        if (mHots.isEmpty() || mHots.size() < 10) return;
        mBinding.hot.setText(mHots.get(new Random().nextInt(11)));
    }

    private void setFabVisible(int position) {
        if (position == 0) {
            mBinding.link.show();
            mBinding.filter.hide();
        } else if (mAdapter.get(position).getFilters().size() > 0) {
            mBinding.filter.show();
            mBinding.link.hide();
        } else {
            mBinding.filter.setVisibility(View.GONE);
            mBinding.link.setVisibility(View.GONE);
        }
    }

    private void onLink(View view) {
        if (ApiConfig.hasPush()) LinkDialog.create(this).show();
        else mBinding.link.hide();
    }

    private void onLogo(View view) {
        SiteDialog.create(this).change().show();
    }

    private void onKeep(View view) {
        KeepActivity.start(getActivity());
    }

    private void onFilter(View view) {
        for (Fragment fragment : getChildFragmentManager().getFragments()) if (fragment instanceof BottomSheetDialogFragment) return;
        FilterDialog.create(this).filter(mAdapter.get(mBinding.pager.getCurrentItem()).getFilters()).show(getChildFragmentManager(), null);
    }

    private void onHot(View view) {
        CollectActivity.start(getActivity());
    }

    private void onSearch(View view) {
        CollectActivity.start(getActivity(), mBinding.hot.getText().toString());
    }

    private void onHistory(View view) {
        HistoryActivity.start(getActivity());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case VIDEO:
            case SIZE:
                homeContent();
                break;
        }
    }

    private void homeContent() {
        setFabVisible(0);
        mAdapter.clear();
        mBinding.pager.setAdapter(new PageAdapter(getChildFragmentManager()));
    }

    private List<Class> getTypes(Result result) {
        List<Class> types = new ArrayList<>();
        for (String cate : getSite().getCategories()) for (Class type : result.getTypes()) if (cate.equals(type.getTypeName())) types.add(type);
        return types;
    }

    @Override
    public void setSite(Site item) {
        ApiConfig.get().setHome(item);
        homeContent();
    }

    @Override
    public void onChanged() {
    }

    @Override
    public void onItemClick(int position, Class item) {
        mBinding.pager.setCurrentItem(position);
        mAdapter.setActivated(position);
    }

    @Override
    public void setFilter(String key, String value) {
        ((TypeFragment) getFragment()).setFilter(key, value);
    }

    public void setAdapter(Result result) {
        try {
            if (mAdapter.hasType()) return;
            result.setTypes(getTypes(result));
            mAdapter.addAll(result.getTypes());
            for (Class item : mAdapter.getTypes()) if (result.getFilters().containsKey(item.getTypeId())) item.setFilters(result.getFilters().get(item.getTypeId()));
            mBinding.pager.getAdapter().notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canBack() {
        if (mBinding.pager.getAdapter() == null) return true;
        return getFragment().canBack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        App.removeCallbacks(mRunnable);
        EventBus.getDefault().unregister(this);
    }

    class PageAdapter extends FragmentStatePagerAdapter {

        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Class type = mAdapter.get(position);
            if (position == 0) return HomeFragment.newInstance();
            return TypeFragment.newInstance(type.getTypeId(), type.getTypeFlag().equals("1"));
        }

        @Override
        public int getCount() {
            return mAdapter.getItemCount();
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        }
    }
}
