package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.RelativeLayout;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Collect;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityCollectBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.adapter.CollectAdapter;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.utils.PauseThreadPoolExecutor;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CollectActivity extends BaseActivity implements CollectAdapter.OnClickListener, VodAdapter.OnClickListener {

    private PauseThreadPoolExecutor mExecutor;
    private ActivityCollectBinding mBinding;
    private CollectAdapter mCollectAdapter;
    private SiteViewModel mViewModel;
    private VodAdapter mVodAdapter;
    private List<Site> mSites;

    public static void start(Activity activity) {
        start(activity, "");
    }

    public static void start(Activity activity, String keyword) {
        Intent intent = new Intent(activity, CollectActivity.class);
        intent.putExtra("keyword", keyword);
        activity.startActivity(intent);
    }

    private String getKeyword() {
        return getIntent().getStringExtra("keyword");
    }

    private boolean empty() {
        return mBinding.keyword.getText().toString().trim().isEmpty();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCollectBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        setRecyclerView();
        setLayoutSize();
        setViewModel();
        setKeyword();
        setSite();
        search();
    }

    @Override
    protected void initEvent() {
        mBinding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) search();
            return true;
        });
    }

    private void setLayoutSize() {
        int width = (ResUtil.getScreenWidthPx() - ResUtil.dp2px(64)) / 3;
        int height = (int) (width / 0.75f);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mBinding.collect.getLayoutParams();
        params.width = width + ResUtil.dp2px(24);
        mBinding.collect.setLayoutParams(params);
        mVodAdapter.setWidth(width);
        mVodAdapter.setHeight(height);
    }

    private void setRecyclerView() {
        mBinding.collect.setHasFixedSize(true);
        mBinding.collect.setItemAnimator(null);
        mBinding.collect.setAdapter(mCollectAdapter = new CollectAdapter(this));
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setAdapter(mVodAdapter = new VodAdapter(this));
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, 2));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.search.observe(this, result -> {
            if (mCollectAdapter.getPosition() == 0) mVodAdapter.addAll(result.getList());
            mCollectAdapter.add(Collect.create(result.getList()));
            mCollectAdapter.add(result.getList());
        });
    }

    private void setKeyword() {
        if (TextUtils.isEmpty(getKeyword())) mBinding.keyword.requestFocus();
        else mBinding.keyword.setText(getKeyword());
    }

    private void setSite() {
        mSites = new ArrayList<>();
        for (Site site : ApiConfig.get().getSites()) if (site.isSearchable()) mSites.add(site);
        Site home = ApiConfig.get().getHome();
        if (!mSites.contains(home)) return;
        mSites.remove(home);
        mSites.add(0, home);
    }

    private void search() {
        if (empty()) return;
        mVodAdapter.clear();
        mCollectAdapter.clear();
        Utils.hideKeyboard(mBinding.keyword);
        int core = Runtime.getRuntime().availableProcessors();
        int corePoolSize = Math.max(Constant.THREAD_POOL, core);
        mExecutor = new PauseThreadPoolExecutor(corePoolSize, corePoolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        for (Site site : mSites) mExecutor.execute(() -> search(site));
    }

    private void search(Site site) {
        try {
            mViewModel.searchContent(site, mBinding.keyword.getText().toString());
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onItemClick(int position, Collect item) {
        mBinding.recycler.scrollToPosition(0);
        mCollectAdapter.setActivated(position);
        mVodAdapter.clear();
        mVodAdapter.addAll(item.getList());
    }

    @Override
    public void onItemClick(Vod item) {
        DetailActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName());
    }

    @Override
    public boolean onLongClick(Vod item) {
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mExecutor != null) mExecutor.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mExecutor != null) mExecutor.pause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mExecutor != null) mExecutor.shutdownNow();
    }
}
