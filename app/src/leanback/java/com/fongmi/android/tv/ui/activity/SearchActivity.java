package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivitySearchBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.presenter.TitlePresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends BaseActivity implements VodPresenter.OnClickListener {

    private ActivitySearchBinding mBinding;
    private SiteViewModel mSiteViewModel;
    private ArrayObjectAdapter mAdapter;
    private ExecutorService mService;
    private List<Site> mSites;

    public static void start(Activity activity) {
        start(activity, "");
    }

    public static void start(Activity activity, String keyword) {
        Intent intent = new Intent(activity, SearchActivity.class);
        intent.putExtra("keyword", keyword);
        //activity.startActivity(intent);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySearchBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        setRecyclerView();
        setViewModel();
        setSite();
    }

    @Override
    protected void initEvent() {
        mBinding.search.setOnClickListener(view -> startSearch());
        mBinding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) mBinding.search.performClick();
            return true;
        });
    }

    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(String.class, new TitlePresenter());
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.result.observe(this, this::addResult);
    }

    private void setSite() {
        mSites = new ArrayList<>();
        for (Site site : ApiConfig.get().getSites()) if (site.isSearchable()) mSites.add(site);
    }

    private void addResult(Result result) {
        List<ListRow> rows = new ArrayList<>();
        for (List<Vod> items : Lists.partition(result.getList(), 5)) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new VodPresenter(this, 5));
            adapter.addAll(0, items);
            rows.add(new ListRow(adapter));
        }
        mAdapter.add(result.getList().get(0).getSite().getName());
        mAdapter.addAll(mAdapter.size(), rows);
    }

    private void startSearch() {
        String keyword = mBinding.keyword.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) return;
        mService = Executors.newFixedThreadPool(5);
        for (Site site : mSites) mService.execute(() -> mSiteViewModel.searchContent(site.getKey(), keyword));
        hideLayout();
    }

    private void stopSearch() {
        if (mService != null) {
            mService.shutdownNow();
            mService = null;
        }
    }

    private void showLayout() {
        mAdapter.clear();
        mBinding.layout.setVisibility(View.VISIBLE);
        mBinding.recycler.setVisibility(View.INVISIBLE);
    }

    private void hideLayout() {
        mBinding.keyword.setText("");
        mBinding.layout.setVisibility(View.GONE);
        mBinding.recycler.setVisibility(View.VISIBLE);
    }

    @Override
    public void onItemClick(Vod item) {
        DetailActivity.start(this, item.getSite().getKey(), item.getVodId());
    }

    @Override
    public void onBackPressed() {
        if (mBinding.recycler.getVisibility() == View.VISIBLE) {
            stopSearch();
            showLayout();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSearch();
    }
}
