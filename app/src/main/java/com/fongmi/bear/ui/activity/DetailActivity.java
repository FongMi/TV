package com.fongmi.bear.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlight;
import androidx.leanback.widget.FocusHighlightHelper;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.bear.ApiConfig;
import com.fongmi.bear.R;
import com.fongmi.bear.bean.Vod;
import com.fongmi.bear.databinding.ActivityDetailBinding;
import com.fongmi.bear.model.SiteViewModel;
import com.fongmi.bear.ui.presenter.ItemPresenter;
import com.fongmi.bear.utils.ResUtil;

import java.util.Arrays;

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
            mBinding.name.setText(item.getVodName());
            setText(mBinding.year, R.string.detail_year, item.getVodYear());
            setText(mBinding.area, R.string.detail_area, item.getVodArea());
            setText(mBinding.type, R.string.detail_type, item.getTypeName());
            setText(mBinding.actor, R.string.detail_actor, item.getVodActor());
            setText(mBinding.content, R.string.detail_content, item.getVodContent());
            setText(mBinding.director, R.string.detail_director, item.getVodDirector());
            setText(mBinding.site, R.string.detail_site, ApiConfig.get().getHome().getName());
            mBinding.from.setHorizontalSpacing(ResUtil.dp2px(16));
            mBinding.from.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new ItemPresenter());
            adapter.addAll(0, Arrays.asList(item.getVodPlayFrom().split("\\$\\$\\$")));
            ItemBridgeAdapter bridgeAdapter = new ItemBridgeAdapter(adapter);
            mBinding.from.setAdapter(bridgeAdapter);
            FocusHighlightHelper.setupBrowseItemFocusHighlight(bridgeAdapter, FocusHighlight.ZOOM_FACTOR_MEDIUM, false);
        });
    }

    private void setPlayer() {

    }

    private void setText(TextView view, int resId, String text) {
        if (text.isEmpty()) view.setVisibility(View.GONE);
        else view.setText(ResUtil.getString(resId, text));
    }

    private void getDetail() {
        mSiteViewModel.detailContent(getId());
    }

    private void getPlayer(String flag, String id) {
        mSiteViewModel.playerContent(flag, id);
    }
}
