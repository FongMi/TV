package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.ViewGroup;

import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.bean.Result;
import com.fongmi.bear.databinding.ActivityVodBinding;
import com.fongmi.bear.ui.adapter.PageAdapter;
import com.fongmi.bear.ui.adapter.TypeAdapter;
import com.fongmi.bear.utils.ResUtil;

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
        });
    }

    private void setRecyclerView() {
        mBinding.recycler.setNumRows(1);
        mBinding.recycler.setHorizontalSpacing(ResUtil.dp2px(12));
        mBinding.recycler.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.recycler.setAdapter(mTypeAdapter = new TypeAdapter(mResult.getTypes()));
    }

    private void setPager() {
        mBinding.pager.setAdapter(new PageAdapter(getSupportFragmentManager(), mResult));
        if (mResult.getTypes().size() > 0) mBinding.pager.setOffscreenPageLimit(mResult.getTypes().size());
    }
}
