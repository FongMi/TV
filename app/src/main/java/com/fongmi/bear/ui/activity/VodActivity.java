package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.bean.Result;
import com.fongmi.bear.databinding.ActivityVodBinding;
import com.fongmi.bear.ui.adapter.PageAdapter;
import com.fongmi.bear.ui.adapter.TypeAdapter;

public class VodActivity extends BaseActivity {

    private ActivityVodBinding mBinding;
    private TypeAdapter mTypeAdapter;
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
        setRecyclerView();
        setPager();
    }

    @Override
    protected void initEvent() {
        mTypeAdapter.setOnItemClickListener(position -> {
            mBinding.pager.setCurrentItem(position, false);
            mTypeAdapter.setSelect(position);
        });
    }

    private void setRecyclerView() {
        mBinding.type.setHasFixedSize(true);
        mBinding.type.getItemAnimator().setChangeDuration(0);
        mBinding.type.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mBinding.type.setAdapter(mTypeAdapter = new TypeAdapter(mResult.getTypes()));
        mBinding.type.scrollToPosition(0);
        mTypeAdapter.setSelect(0);
    }

    private void setPager() {
        mBinding.pager.setAdapter(new PageAdapter(this, mResult.getTypes(), mResult.getFilters()));
        mBinding.pager.setOffscreenPageLimit(mResult.getTypes().size());
        mBinding.pager.setUserInputEnabled(false);
    }
}
