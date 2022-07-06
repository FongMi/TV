package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;
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

import com.fongmi.bear.bean.Result;
import com.fongmi.bear.databinding.ActivityVodBinding;
import com.fongmi.bear.ui.fragment.VodFragment;
import com.fongmi.bear.ui.presenter.TypePresenter;
import com.fongmi.bear.utils.ResUtil;

public class VodActivity extends BaseActivity {

    private TypePresenter mTypePresenter;
    private ActivityVodBinding mBinding;
    private Result mResult;
    private View mOldView;

    private String getResult() {
        return getIntent().getStringExtra("result");
    }

    public static void start(Activity activity, Result result) {
        if (result == null || result.getTypes().isEmpty()) return;
        Intent intent = new Intent(activity, VodActivity.class);
        intent.putExtra("result", result.toString());
        activity.startActivity(intent);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityVodBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mResult = Result.objectFrom(getResult());
        setRecyclerView();
        setPager();
    }

    @Override
    protected void initEvent() {
        mTypePresenter.setOnClickListener((item) -> mBinding.pager.setCurrentItem(mResult.getTypes().indexOf(item)));
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBinding.recycler.setSelectedPosition(position);
            }
        });
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                mBinding.pager.setCurrentItem(position);
                if (mOldView != null) mOldView.setActivated(false);
                if (child == null) return;
                mOldView = child.itemView;
                mOldView.setActivated(true);
            }
        });
    }

    private void setRecyclerView() {
        mBinding.recycler.setHorizontalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(mTypePresenter = new TypePresenter());
        adapter.addAll(0, mResult.getTypes());
        ItemBridgeAdapter bridgeAdapter = new ItemBridgeAdapter(adapter);
        mBinding.recycler.setAdapter(bridgeAdapter);
    }

    private void setPager() {
        mBinding.pager.setAdapter(new PageAdapter(getSupportFragmentManager()));
        if (mResult.getTypes().size() > 0) mBinding.pager.setOffscreenPageLimit(mResult.getTypes().size());
    }

    class PageAdapter extends FragmentStatePagerAdapter {

        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
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
}
