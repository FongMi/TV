package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.ApiConfig;
import com.fongmi.bear.R;
import com.fongmi.bear.bean.Vod;
import com.fongmi.bear.databinding.ActivityDetailBinding;
import com.fongmi.bear.model.SiteViewModel;
import com.fongmi.bear.utils.ResUtil;

public class DetailActivity extends BaseActivity {

    private ActivityDetailBinding mBinding;
    private SiteViewModel mSiteViewModel;

    private String getId() {
        return getIntent().getStringExtra("id");
    }

    public static void start(Activity activity, String id) {
        Intent intent = new Intent(activity, DetailActivity.class);
        intent.putExtra("id", id);
        activity.startActivity(intent);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityDetailBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.progress.showProgress();
        setRecyclerView();
        setViewModel();
        getDetail();
    }

    @Override
    protected void initEvent() {

    }

    private void setRecyclerView() {

    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.mResult.observe(this, result -> {
            Vod item = result.getList().get(0);
            mBinding.progress.showContent();
            mBinding.site.setText(ApiConfig.get().getHome().getName());
            mBinding.name.setText(item.getVodName());
            mBinding.year.setText(ResUtil.getString(R.string.detail_year, item.getVodYear()));
            mBinding.area.setText(ResUtil.getString(R.string.detail_area, item.getVodArea()));
            mBinding.type.setText(ResUtil.getString(R.string.detail_type, item.getTypeName()));
            mBinding.actor.setText(ResUtil.getString(R.string.detail_actor, item.getVodActor()));
            mBinding.content.setText(ResUtil.getString(R.string.detail_content, item.getVodContent()));
            mBinding.director.setText(ResUtil.getString(R.string.detail_director, item.getVodDirector()));
        });
    }

    private void getDetail() {
        mSiteViewModel.detailContent(getId());
    }
}
