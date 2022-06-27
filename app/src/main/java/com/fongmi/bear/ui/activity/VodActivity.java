package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.fongmi.bear.bean.Result;
import com.fongmi.bear.bean.Type;
import com.fongmi.bear.databinding.ActivityVodBinding;
import com.fongmi.bear.ui.fragment.VodFragment;
import com.google.android.material.tabs.TabLayout;

public class VodActivity extends BaseActivity {

    private ActivityVodBinding mBinding;
    private Result mResult;

    private String getResult() {
        return getIntent().getStringExtra("result");
    }

    public static void start(Activity activity, Result result) {
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
        for (Type type : mResult.getTypes()) mBinding.tab.addTab(mBinding.tab.newTab().setText(type.getTypeName()));
        mBinding.pager.setAdapter(new PageAdapter(this));
        mBinding.pager.setOffscreenPageLimit(mResult.getTypes().size());
    }

    @Override
    protected void initEvent() {
        mBinding.tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mBinding.pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    public class PageAdapter extends FragmentStateAdapter {

        public PageAdapter(@NonNull FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return VodFragment.newInstance(mResult.getTypes().get(position).getTypeId());
        }

        @Override
        public int getItemCount() {
            return mResult.getTypes().size();
        }
    }
}
