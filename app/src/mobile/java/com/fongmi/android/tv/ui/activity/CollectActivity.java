package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Collect;
import com.fongmi.android.tv.bean.Hot;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Suggest;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityCollectBinding;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.net.OkHttp;
import com.fongmi.android.tv.ui.adapter.CollectAdapter;
import com.fongmi.android.tv.ui.adapter.RecordAdapter;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.adapter.WordAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.ui.custom.ViewType;
import com.fongmi.android.tv.ui.custom.dialog.SiteDialog;
import com.fongmi.android.tv.utils.PauseThreadPoolExecutor;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Response;

public class CollectActivity extends BaseActivity implements SiteCallback, WordAdapter.OnClickListener, RecordAdapter.OnClickListener, CollectAdapter.OnClickListener, VodAdapter.OnClickListener {

    private PauseThreadPoolExecutor mExecutor;
    private ActivityCollectBinding mBinding;
    private CollectAdapter mCollectAdapter;
    private RecordAdapter mRecordAdapter;
    private WordAdapter mWordAdapter;
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
    protected void initView(Bundle savedInstanceState) {
        mSites = new ArrayList<>();
        setRecyclerView();
        setLayoutSize();
        setViewModel();
        setViewType();
        setKeyword();
        setSite();
        getHot();
        search();
    }

    @Override
    protected void initEvent() {
        mBinding.view.setOnClickListener(this::toggleView);
        mBinding.site.setOnClickListener(v -> SiteDialog.create(this).search().show());
        mBinding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) search();
            return true;
        });
        mBinding.keyword.addTextChangedListener(new CustomTextListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) getHot();
                else getSuggest(s.toString());
            }
        });
    }

    private void setRecyclerView() {
        mBinding.collect.setHasFixedSize(true);
        mBinding.collect.setItemAnimator(null);
        mBinding.collect.setAdapter(mCollectAdapter = new CollectAdapter(this));
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setAdapter(mVodAdapter = new VodAdapter(this));
        mBinding.wordRecycler.setHasFixedSize(true);
        mBinding.wordRecycler.setAdapter(mWordAdapter = new WordAdapter(this));
        mBinding.recordRecycler.setHasFixedSize(true);
        mBinding.recordRecycler.setAdapter(mRecordAdapter = new RecordAdapter(this));
        mVodAdapter.setSize(Product.getSpec(this, ResUtil.dp2px(64), 3));
    }

    private void setViewType() {
        mVodAdapter.setViewType(Prefers.getViewType());
        boolean grid = mVodAdapter.getViewType() == ViewType.GRID;
        GridLayoutManager manager = (GridLayoutManager) mBinding.recycler.getLayoutManager();
        mBinding.view.setImageResource(grid ? R.drawable.ic_view_list : R.drawable.ic_view_grid);
        manager.setSpanCount(grid ? 2 : 1);
    }

    private void setLayoutSize() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mBinding.collect.getLayoutParams();
        params.width = mVodAdapter.getWidth() + ResUtil.dp2px(24);
        mBinding.collect.setLayoutParams(params);
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
        mBinding.site.setVisibility(View.GONE);
        mBinding.agent.setVisibility(View.GONE);
        mBinding.view.setVisibility(View.VISIBLE);
        mBinding.result.setVisibility(View.VISIBLE);
        if (mExecutor != null) mExecutor.shutdownNow();
        mExecutor = new PauseThreadPoolExecutor(Constant.THREAD_POOL, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        String keyword = mBinding.keyword.getText().toString().trim();
        for (Site site : mSites) mExecutor.execute(() -> search(site, keyword));
        App.post(() -> mRecordAdapter.add(keyword), 250);
    }

    private void search(Site site, String keyword) {
        try {
            mViewModel.searchContent(site, keyword);
        } catch (Throwable ignored) {
        }
    }

    private void getHot() {
        mBinding.word.setText(R.string.search_hot);
        mWordAdapter.addAll(Hot.get(Prefers.getHot()));
    }

    private void getSuggest(String text) {
        mBinding.word.setText(R.string.search_suggest);
        OkHttp.newCall("https://suggest.video.iqiyi.com/?if=mobile&key=" + text).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                List<String> items = Suggest.get(response.body().string());
                App.post(() -> mWordAdapter.addAll(items));
            }
        });
    }

    private void toggleView(View view) {
        mVodAdapter.setViewType(mVodAdapter.getViewType() == ViewType.GRID ? ViewType.LIST : ViewType.GRID);
        Prefers.putViewType(mVodAdapter.getViewType());
        setViewType();
    }

    private void showAgent() {
        mVodAdapter.clear();
        mCollectAdapter.clear();
        mBinding.view.setVisibility(View.GONE);
        mBinding.result.setVisibility(View.GONE);
        mBinding.site.setVisibility(View.VISIBLE);
        mBinding.agent.setVisibility(View.VISIBLE);
        if (mExecutor != null) mExecutor.shutdownNow();
    }

    @Override
    public void setSite(Site item) {
    }

    @Override
    public void onChanged() {
        mSites.clear();
        setSite();
    }

    @Override
    public void onItemClick(String text) {
        mBinding.keyword.setText(text);
        mBinding.keyword.setSelection(text.length());
        search();
    }

    @Override
    public void onDataChanged(int size) {
        mBinding.record.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
        mBinding.recordRecycler.setVisibility(size == 0 ? View.GONE : View.VISIBLE);
        App.post(() -> mBinding.recordRecycler.requestLayout(), 250);
    }

    @Override
    public void onItemClick(int position, Collect item) {
        mBinding.recycler.scrollToPosition(0);
        mCollectAdapter.setActivated(position);
        mVodAdapter.clear().addAll(item.getList());
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
        if (isVisible(mBinding.result)) {
            showAgent();
        } else {
            super.onBackPressed();
        }
    }
}
