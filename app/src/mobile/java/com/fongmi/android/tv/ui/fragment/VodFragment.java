package com.fongmi.android.tv.ui.fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.databinding.FragmentVodBinding;
import com.fongmi.android.tv.ui.activity.BaseFragment;
import com.fongmi.android.tv.ui.adapter.TypeAdapter;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class VodFragment extends BaseFragment {

    private FragmentVodBinding mBinding;
    private TypeAdapter mAdapter;
    private Result mResult;

    public static VodFragment newInstance() {
        return new VodFragment();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentVodBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setAdapter(mAdapter = new TypeAdapter());
        mBinding.pager.setAdapter(new PageAdapter(getChildFragmentManager()));
    }

    @Override
    protected void initEvent() {
        EventBus.getDefault().register(this);
        mAdapter.setListener(item -> mBinding.pager.setCurrentItem(mAdapter.setActivated(item)));
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBinding.recycler.smoothScrollToPosition(position);
                mAdapter.setActivated(position);
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResult(Result result) {
        setAdapter(mResult = result);
    }

    private void setAdapter(Result result) {
        result.setTypes(getTypes());
        mAdapter.addAll(result.getTypes());
        Boolean filter = ApiConfig.get().getHome().isFilterable() ? false : null;
        for (Class item : mAdapter.getTypes()) if (result.getFilters().containsKey(item.getTypeId())) item.setFilter(filter);
        mBinding.pager.getAdapter().notifyDataSetChanged();
    }

    private List<Class> getTypes() {
        List<Class> types = new ArrayList<>();
        for (String cate : ApiConfig.get().getHome().getCategories()) for (Class item : mResult.getTypes()) if (cate.equals(item.getTypeName())) types.add(item);
        return types;
    }

    class PageAdapter extends FragmentStatePagerAdapter {

        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Class type = mAdapter.get(position);
            String filter = new Gson().toJson(mResult.getFilters().get(type.getTypeId()));
            return ChildFragment.newInstance(type.getTypeId(), filter, type.getTypeFlag().equals("1"));
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
