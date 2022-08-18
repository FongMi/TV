package com.fongmi.android.tv.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Func;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.presenter.FuncPresenter;
import com.fongmi.android.tv.ui.presenter.HistoryPresenter;
import com.fongmi.android.tv.ui.presenter.ProgressPresenter;
import com.fongmi.android.tv.ui.presenter.TitlePresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Updater;
import com.google.common.collect.Lists;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends BaseActivity implements VodPresenter.OnClickListener, FuncPresenter.OnClickListener, HistoryPresenter.OnClickListener {

    private ActivityHomeBinding mBinding;
    private ArrayObjectAdapter mAdapter;
    private ArrayObjectAdapter mHistoryAdapter;
    private HistoryPresenter mHistoryPresenter;
    private FuncPresenter mFuncPresenter;
    private SiteViewModel mSiteViewModel;
    private boolean mConfirmExit;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, HomeActivity.class));
        activity.finish();
        Updater.check();
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        Clock.start(mBinding.time);
        Server.get().start();
        Players.get().init();
        setRecyclerView();
        setViewModel();
        setAdapter();
        getHistory();
        getVideo();
    }

    @Override
    protected void initEvent() {
        EventBus.getDefault().register(this);
        mFuncPresenter.setOnClickListener(this);
        mHistoryPresenter.setOnClickListener(this);
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                mBinding.time.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(Integer.class, new TitlePresenter());
        selector.addPresenter(String.class, new ProgressPresenter());
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), FuncPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), HistoryPresenter.class);
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mHistoryAdapter = new ArrayObjectAdapter(mHistoryPresenter = new HistoryPresenter());
    }

    private void setViewModel() {
        mSiteViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mSiteViewModel.result.observe(this, result -> {
            mAdapter.remove("progress");
            if (result != null) addVideo(result);
        });
    }

    private void setAdapter() {
        mAdapter.add(R.string.app_name);
        mAdapter.add(getFuncRow());
        mAdapter.add(R.string.home_history);
        mAdapter.add(R.string.home_recommend);
    }

    private void getVideo() {
        if (mAdapter.size() > getRecommendIndex()) mAdapter.removeItems(getRecommendIndex(), mAdapter.size() - getRecommendIndex());
        if (ApiConfig.get().getHome().getKey().isEmpty()) return;
        mSiteViewModel.homeContent();
        mAdapter.add("progress");
    }

    private void addVideo(Result result) {
        List<ListRow> rows = new ArrayList<>();
        for (List<Vod> items : Lists.partition(result.getList(), 5)) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new VodPresenter(this));
            adapter.setItems(items, null);
            rows.add(new ListRow(adapter));
        }
        mAdapter.addAll(mAdapter.size(), rows);
    }

    private ListRow getFuncRow() {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(mFuncPresenter = new FuncPresenter());
        adapter.add(Func.create(R.string.home_vod));
        adapter.add(Func.create(R.string.home_live));
        adapter.add(Func.create(R.string.home_search));
        adapter.add(Func.create(R.string.home_push));
        adapter.add(Func.create(R.string.home_setting));
        return new ListRow(adapter);
    }

    private void getHistory() {
        int historyIndex = getHistoryIndex();
        int recommendIndex = getRecommendIndex();
        boolean isExist = recommendIndex - historyIndex == 2;
        List<History> items = History.find(ApiConfig.getCid());
        if (items.isEmpty() && isExist) mAdapter.removeItems(getHistoryIndex(), 1);
        if (items.size() > 0 && !isExist) mAdapter.add(historyIndex, new ListRow(mHistoryAdapter));
        mHistoryAdapter.setItems(items, null);
    }

    private int getHistoryIndex() {
        for (int i = 0; i < mAdapter.size(); i++) if (mAdapter.get(i).equals(R.string.home_history)) return i + 1;
        return -1;
    }

    private int getRecommendIndex() {
        for (int i = 0; i < mAdapter.size(); i++) if (mAdapter.get(i).equals(R.string.home_recommend)) return i + 1;
        return -1;
    }

    @Override
    public void onItemClick(Func item) {
        switch (item.getResId()) {
            case R.string.home_vod:
                VodActivity.start(this, mSiteViewModel.getResult().getValue());
                break;
            case R.string.home_search:
                SearchActivity.start(this);
                break;
            case R.string.home_push:
                PushActivity.start(this);
                break;
            case R.string.home_setting:
                SettingActivity.start(this);
                break;
        }
    }

    @Override
    public void onItemClick(Vod item) {
        DetailActivity.start(this, item.getVodId());
    }

    @Override
    public void onItemClick(History item) {
        DetailActivity.start(this, item.getSiteKey(), item.getVodId());
    }

    @Override
    public void onItemDelete(History item) {
        mHistoryAdapter.remove(item.delete());
        if (mHistoryAdapter.size() > 0) return;
        mAdapter.removeItems(getHistoryIndex(), 1);
        mHistoryPresenter.setDelete(false);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onLongClick() {
        mHistoryPresenter.setDelete(true);
        mHistoryAdapter.notifyArrayItemRangeChanged(0, mHistoryAdapter.size());
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.VIDEO) {
            getVideo();
        } else if (event.getType() == RefreshEvent.Type.IMAGE) {
            mAdapter.notifyArrayItemRangeChanged(getRecommendIndex(), mAdapter.size() - getRecommendIndex());
        } else if (event.getType() == RefreshEvent.Type.HISTORY) {
            getHistory();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        switch (event.getType()) {
            case SEARCH:
                SearchActivity.start(this, event.getText());
                break;
            case PUSH:
                if (ApiConfig.get().getSite("push_agent") == null) return;
                DetailActivity.start(this, "push_agent", event.getText());
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (mHistoryPresenter.isDelete()) {
            mHistoryPresenter.setDelete(false);
            mHistoryAdapter.notifyArrayItemRangeChanged(0, mHistoryAdapter.size());
        } else if (mBinding.recycler.getSelectedPosition() != 1) {
            mBinding.recycler.smoothScrollToPosition(1);
        } else if (!mConfirmExit) {
            mConfirmExit = true;
            Notify.show(R.string.app_exit);
            new Handler().postDelayed(() -> mConfirmExit = false, 1000);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Server.get().stop();
        Clock.get().release();
        Players.get().release();
        ApiConfig.get().release();
        EventBus.getDefault().unregister(this);
    }
}