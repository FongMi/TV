package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.R;
import com.fongmi.bear.bean.Func;
import com.fongmi.bear.bean.Result;
import com.fongmi.bear.databinding.ActivityHomeBinding;
import com.fongmi.bear.model.SiteViewModel;
import com.fongmi.bear.ui.adapter.FuncAdapter;
import com.fongmi.bear.ui.adapter.VodAdapter;
import com.fongmi.bear.ui.custom.SpaceItemDecoration;

public class HomeActivity extends BaseActivity {

    private ActivityHomeBinding mBinding;
    private SiteViewModel mSiteViewModel;
    private FuncAdapter mFuncAdapter;
    private VodAdapter mVodAdapter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, HomeActivity.class));
        activity.finish();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        setRecyclerView();
        setViewModel();
        getContent();
    }

    @Override
    protected void initEvent() {
        mFuncAdapter.setOnItemClickListener(this::onFuncClick);
    }

    private void setRecyclerView() {
        mBinding.func.setHasFixedSize(true);
        mBinding.func.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mBinding.func.setAdapter(mFuncAdapter = new FuncAdapter());
        mBinding.recommend.setHasFixedSize(true);
        mBinding.recommend.setLayoutManager(new GridLayoutManager(this, 5));
        mBinding.recommend.addItemDecoration(new SpaceItemDecoration(5, 12, false, 0));
        mBinding.recommend.setAdapter(mVodAdapter = new VodAdapter());
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.mResult.observe(this, result -> {
            mVodAdapter.addAll(result.getList());
            mBinding.progress.showContent();
        });
    }

    private void getContent() {
        mBinding.progress.showProgress();
        mSiteViewModel.homeContent();
    }

    private void onFuncClick(Func item) {
        switch (item.getResId()) {
            case R.string.home_vod:
                Result result = mSiteViewModel.getResult().getValue();
                if (result != null) VodActivity.start(this, result);
                break;
            case R.string.home_setting:
                SettingActivity.start(this);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        getContent();
    }
}