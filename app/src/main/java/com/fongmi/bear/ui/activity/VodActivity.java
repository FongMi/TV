package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.fongmi.bear.bean.Result;
import com.fongmi.bear.databinding.ActivityVodBinding;
import com.fongmi.bear.ui.adapter.PageAdapter;
import com.fongmi.bear.ui.presenter.TypePresenter;
import com.fongmi.bear.utils.ResUtil;

public class VodActivity extends BaseActivity {

    private ActivityVodBinding mBinding;
    private TypePresenter mTypePresenter;
    private Result mResult;
    private View mOldView;

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
                if (mOldView != null) mOldView.setSelected(false);
                mOldView = child.itemView;
                mOldView.setSelected(true);
            }
        });
    }

    private void setRecyclerView() {
        mBinding.recycler.setHorizontalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        ArrayObjectAdapter arrayObjectAdapter = new ArrayObjectAdapter(mTypePresenter = new TypePresenter());
        arrayObjectAdapter.addAll(0, mResult.getTypes());
        ItemBridgeAdapter adapter = new ItemBridgeAdapter(arrayObjectAdapter);
        mBinding.recycler.setAdapter(adapter);
    }

    private void setPager() {
        mBinding.pager.setAdapter(new PageAdapter(getSupportFragmentManager(), mResult));
        if (mResult.getTypes().size() > 0) mBinding.pager.setOffscreenPageLimit(mResult.getTypes().size());
    }
}
