package com.fongmi.android.tv.ui.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.FragmentVodBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.BaseFragment;
import com.fongmi.android.tv.ui.adapter.TypeAdapter;
import com.fongmi.android.tv.ui.custom.dialog.SiteDialog;
import com.fongmi.android.tv.ui.fragment.child.SiteFragment;
import com.fongmi.android.tv.ui.fragment.child.TypeFragment;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class VodFragment extends BaseFragment implements SiteCallback, TypeAdapter.OnClickListener {

    private FragmentVodBinding mBinding;
    private SiteViewModel mViewModel;
    private TypeAdapter mTypeAdapter;
    private PageAdapter mPageAdapter;
    private boolean destroy;

    public static VodFragment newInstance() {
        return new VodFragment();
    }

    private Site getSite() {
        return ApiConfig.get().getHome();
    }

    public boolean isDestroy() {
        return destroy;
    }

    public void setDestroy(boolean destroy) {
        this.destroy = destroy;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentVodBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setRecyclerView();
        setViewModel();
    }

    @Override
    protected void initEvent() {
        mBinding.title.setOnClickListener(this::onTitle);
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBinding.type.smoothScrollToPosition(position);
                mTypeAdapter.setActivated(position);
            }
        });
    }

    private void setRecyclerView() {
        mBinding.type.setHasFixedSize(true);
        mBinding.type.setItemAnimator(null);
        mBinding.type.setAdapter(mTypeAdapter = new TypeAdapter(this));
        mBinding.pager.setAdapter(mPageAdapter = new PageAdapter(getChildFragmentManager()));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(getViewLifecycleOwner(), result -> {
            EventBus.getDefault().post(result);
            mPageAdapter.setResult(result);
            setAdapter(result);
            setDestroy(false);
        });
    }

    private List<Class> getTypes(Result result) {
        List<Class> types = new ArrayList<>();
        for (String cate : getSite().getCategories()) for (Class type : result.getTypes()) if (cate.equals(type.getTypeName())) types.add(type);
        return types;
    }

    private void setAdapter(Result result) {
        result.setTypes(getTypes(result));
        mTypeAdapter.addAll(result.getTypes());
        Boolean filter = getSite().isFilterable() ? false : null;
        for (Class item : mTypeAdapter.getTypes()) if (result.getFilters().containsKey(item.getTypeId())) item.setFilter(filter);
        mPageAdapter.notifyDataSetChanged();
        mBinding.pager.setCurrentItem(0);
    }

    private void onTitle(View view) {
        SiteDialog.create(this).filter(true).show();
    }

    @Override
    public void setSite(Site item) {
        ApiConfig.get().setHome(item);
        RefreshEvent.video();
    }

    @Override
    public void onItemClick(int position, Class item) {
        if (position == mBinding.pager.getCurrentItem()) {
            updateFilter(position, item);
        } else {
            mTypeAdapter.setActivated(position);
            mBinding.pager.setCurrentItem(position);
        }
    }

    private void updateFilter(int position, Class item) {
        if (item.getFilter() == null) return;
        getFragment().toggleFilter(item.toggleFilter());
        mTypeAdapter.notifyItemChanged(position);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.VIDEO) homeContent();
    }

    private void homeContent() {
        setDestroy(true);
        mTypeAdapter.clear();
        mPageAdapter.notifyDataSetChanged();
        String home = getSite().getName();
        mBinding.title.setText(home.isEmpty() ? ResUtil.getString(R.string.app_name) : home);
        if (!getSite().getKey().isEmpty()) mViewModel.homeContent();
    }

    private TypeFragment getFragment() {
        return (TypeFragment) mPageAdapter.instantiateItem(mBinding.pager, mBinding.pager.getCurrentItem());
    }

    class PageAdapter extends FragmentStatePagerAdapter {

        private Result result;

        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        public void setResult(Result result) {
            this.result = result;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Class type = mTypeAdapter.get(position);
            if (position == 0) return SiteFragment.newInstance();
            String filter = new Gson().toJson(result.getFilters().get(type.getTypeId()));
            return TypeFragment.newInstance(type.getTypeId(), filter, type.getTypeFlag().equals("1"));
        }

        @Override
        public int getCount() {
            return mTypeAdapter.getItemCount();
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            if (position != 0 && isDestroy()) super.destroyItem(container, position, object);
        }
    }
}
