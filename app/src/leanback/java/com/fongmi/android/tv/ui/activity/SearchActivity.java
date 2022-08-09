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
import com.fongmi.android.tv.ui.presenter.KeyboardPresenter;
import com.fongmi.android.tv.ui.presenter.TitlePresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchActivity extends BaseActivity implements VodPresenter.OnClickListener, KeyboardPresenter.OnClickListener {

    private ActivitySearchBinding mBinding;
    private ArrayObjectAdapter mSearchAdapter;
    private ArrayObjectAdapter mKeyboardAdapter;
    private SiteViewModel mSiteViewModel;
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
        setKeyboard();
        setSite();
    }

    @Override
    protected void initEvent() {
        mBinding.search.setOnClickListener(view -> onSearch());
        mBinding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) mBinding.search.performClick();
            return true;
        });
    }

    private void setRecyclerView() {
        CustomSelector searchSelector = new CustomSelector();
        CustomSelector keyboardSelector = new CustomSelector();
        searchSelector.addPresenter(String.class, new TitlePresenter());
        searchSelector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        keyboardSelector.addPresenter(ListRow.class, new CustomRowPresenter(12), KeyboardPresenter.class);
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        mBinding.keyboard.setVerticalSpacing(ResUtil.dp2px(12));
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mSearchAdapter = new ArrayObjectAdapter(searchSelector)));
        mBinding.keyboard.setAdapter(new ItemBridgeAdapter(mKeyboardAdapter = new ArrayObjectAdapter(keyboardSelector)));
    }

    private void setKeyboard() {
        List<ListRow> rows = new ArrayList<>();
        List<String> keys = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "◁", "▷", "⌫", "⏎");
        for (List<String> items : Lists.partition(keys, 10)) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new KeyboardPresenter(this));
            adapter.addAll(0, items);
            rows.add(new ListRow(adapter));
        }
        mKeyboardAdapter.addAll(mKeyboardAdapter.size(), rows);
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.result.observe(this, this::addVideo);
    }

    private void setSite() {
        mSites = new ArrayList<>();
        for (Site site : ApiConfig.get().getSites()) if (site.isSearchable()) mSites.add(site);
        if (!mSites.contains(ApiConfig.get().getHome())) return;
        mSites.remove(ApiConfig.get().getHome());
        mSites.add(0, ApiConfig.get().getHome());
    }

    private void addVideo(Result result) {
        List<ListRow> rows = new ArrayList<>();
        for (List<Vod> items : Lists.partition(result.getList(), 5)) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new VodPresenter(this, 5));
            adapter.addAll(0, items);
            rows.add(new ListRow(adapter));
        }
        mSearchAdapter.add(result.getList().get(0).getSite().getName());
        mSearchAdapter.addAll(mSearchAdapter.size(), rows);
        mBinding.progressLayout.showContent();
    }

    private void onSearch() {
        String keyword = mBinding.keyword.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) return;
        mService = Executors.newFixedThreadPool(5);
        for (Site site : mSites) mService.execute(() -> mSiteViewModel.searchContent(site.getKey(), keyword));
        showProgress();
    }

    private void stopSearch() {
        if (mService == null) return;
        mService.shutdownNow();
        mService = null;
    }

    private void showProgress() {
        mBinding.keyword.setText("");
        mBinding.progressLayout.showProgress();
        mBinding.layout.setVisibility(View.GONE);
        mBinding.progressLayout.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mSearchAdapter.clear();
        mBinding.layout.setVisibility(View.VISIBLE);
        mBinding.progressLayout.setVisibility(View.INVISIBLE);
    }

    private boolean isProgressVisible() {
        return mBinding.progressLayout.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onItemClick(Vod item) {
        DetailActivity.start(this, item.getSite().getKey(), item.getVodId());
    }

    @Override
    public void onItemClick(String item) {
        StringBuilder sb;
        int cursor = mBinding.keyword.getSelectionStart();
        switch (item) {
            case "⏎":
                mBinding.search.performClick();
                break;
            case "◁":
                mBinding.keyword.setSelection(--cursor < 0 ? 0 : cursor);
                break;
            case "▷":
                mBinding.keyword.setSelection(++cursor > mBinding.keyword.length() ? mBinding.keyword.length() : cursor);
                break;
            case "⌫":
                if (cursor == 0) return;
                sb = new StringBuilder(mBinding.keyword.getText().toString());
                sb.deleteCharAt(cursor - 1);
                mBinding.keyword.setText(sb.toString());
                mBinding.keyword.setSelection(cursor - 1);
                break;
            default:
                sb = new StringBuilder(mBinding.keyword.getText().toString());
                sb.insert(cursor, item);
                mBinding.keyword.setText(sb.toString());
                mBinding.keyword.setSelection(cursor + 1);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (isProgressVisible()) {
            hideProgress();
            stopSearch();
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
