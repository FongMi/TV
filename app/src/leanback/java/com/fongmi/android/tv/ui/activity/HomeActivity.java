package com.fongmi.android.tv.ui.activity;

import android.view.KeyEvent;
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

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.api.Updater;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.bean.Func;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.custom.CustomTitleView;
import com.fongmi.android.tv.ui.custom.dialog.SiteDialog;
import com.fongmi.android.tv.ui.presenter.FuncPresenter;
import com.fongmi.android.tv.ui.presenter.HeaderPresenter;
import com.fongmi.android.tv.ui.presenter.HistoryPresenter;
import com.fongmi.android.tv.ui.presenter.ProgressPresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.Clock;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;
import com.google.common.collect.Lists;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class HomeActivity extends BaseActivity implements CustomTitleView.Listener, VodPresenter.OnClickListener, FuncPresenter.OnClickListener, HistoryPresenter.OnClickListener {

    private ActivityHomeBinding mBinding;
    private ArrayObjectAdapter mAdapter;
    private ArrayObjectAdapter mHistoryAdapter;
    private HistoryPresenter mHistoryPresenter;
    private SiteViewModel mViewModel;
    private boolean confirm;
    private Result result;

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.progressLayout.showProgress();
        Updater.get().start();
        Server.get().start();
        setRecyclerView();
        setViewModel();
        setAdapter();
        initConfig();
    }

    @Override
    protected void initEvent() {
        mBinding.title.setListener(this);
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                mBinding.toolbar.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                if (mHistoryPresenter.isDelete()) setHistoryDelete(false);
            }
        });
    }

    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(Integer.class, new HeaderPresenter());
        selector.addPresenter(String.class, new ProgressPresenter());
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), FuncPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), HistoryPresenter.class);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(this, result -> {
            mAdapter.remove("progress");
            addVideo(this.result = result);
        });
    }

    private void setAdapter() {
        mAdapter.add(getFuncRow());
        mAdapter.add(R.string.home_history);
        mAdapter.add(R.string.home_recommend);
        mHistoryAdapter = new ArrayObjectAdapter(mHistoryPresenter = new HistoryPresenter(this));
    }

    private void initConfig() {
        WallConfig.get().init();
        LiveConfig.get().init();
        ApiConfig.get().init().load(getCallback());
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                mBinding.progressLayout.showContent();
                getHistory();
                getVideo();
                setFocus();
            }

            @Override
            public void error(int resId) {
                mBinding.progressLayout.showContent();
                result = Result.empty();
                Notify.show(resId);
                setFocus();
            }
        };
    }

    private void setFocus() {
        mBinding.recycler.requestFocus();
        App.post(() -> mBinding.title.setFocusable(true), 500);
    }

    private void getVideo() {
        this.result = Result.empty();
        int index = getRecommendIndex();
        String home = ApiConfig.get().getHome().getName();
        mBinding.title.setText(home.isEmpty() ? ResUtil.getString(R.string.app_name) : home);
        if (mAdapter.size() > index) mAdapter.removeItems(index, mAdapter.size() - index);
        if (ApiConfig.get().getHome().getKey().isEmpty()) return;
        mViewModel.homeContent();
        mAdapter.add("progress");
    }

    private void addVideo(Result result) {
        for (List<Vod> items : Lists.partition(result.getList(), Product.getColumn())) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new VodPresenter(this));
            adapter.setItems(items, null);
            mAdapter.add(new ListRow(adapter));
        }
    }

    private ListRow getFuncRow() {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new FuncPresenter(this));
        adapter.add(Func.create(R.string.home_vod));
        adapter.add(Func.create(R.string.home_live));
        adapter.add(Func.create(R.string.home_search));
        adapter.add(Func.create(R.string.home_keep));
        adapter.add(Func.create(R.string.home_push));
        adapter.add(Func.create(R.string.home_setting));
        return new ListRow(adapter);
    }

    private void getHistory() {
        getHistory(false);
    }

    private void getHistory(boolean renew) {
        List<History> items = History.get();
        int historyIndex = getHistoryIndex();
        int recommendIndex = getRecommendIndex();
        boolean exist = recommendIndex - historyIndex == 2;
        if (renew) mHistoryAdapter = new ArrayObjectAdapter(mHistoryPresenter = new HistoryPresenter(this));
        if ((items.isEmpty() && exist) || (renew && exist)) mAdapter.removeItems(historyIndex, 1);
        if ((items.size() > 0 && !exist) || (renew && exist)) mAdapter.add(historyIndex, new ListRow(mHistoryAdapter));
        mHistoryAdapter.setItems(items, null);
    }

    private void setHistoryDelete(boolean delete) {
        mHistoryPresenter.setDelete(delete);
        mHistoryAdapter.notifyArrayItemRangeChanged(0, mHistoryAdapter.size());
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
                VodActivity.start(this, result.clear());
                break;
            case R.string.home_live:
                LiveActivity.start(this);
                break;
            case R.string.home_search:
                SearchActivity.start(this);
                break;
            case R.string.home_keep:
                KeepActivity.start(this);
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
        DetailActivity.start(this, item.getVodId(), item.getVodName());
    }

    @Override
    public boolean onLongClick(Vod item) {
        CollectActivity.start(this, item.getVodName());
        return true;
    }

    @Override
    public void onItemClick(History item) {
        DetailActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName());
    }

    @Override
    public void onItemDelete(History item) {
        mHistoryAdapter.remove(item.delete());
        if (mHistoryAdapter.size() > 0) return;
        mAdapter.removeItems(getHistoryIndex(), 1);
        mHistoryPresenter.setDelete(false);
    }

    @Override
    public boolean onLongClick() {
        setHistoryDelete(true);
        return true;
    }

    @Override
    public void showDialog() {
        SiteDialog.create(this).change().show();
    }

    @Override
    public void setSite(Site item) {
        ApiConfig.get().setHome(item);
        getVideo();
    }

    @Override
    public void onChanged() {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        super.onRefreshEvent(event);
        switch (event.getType()) {
            case VIDEO:
                getVideo();
                break;
            case IMAGE:
                int index = getRecommendIndex();
                mAdapter.notifyArrayItemRangeChanged(index, mAdapter.size() - index);
                break;
            case HISTORY:
                getHistory();
                break;
            case SIZE:
                getVideo();
                getHistory(true);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        switch (event.getType()) {
            case SEARCH:
                CollectActivity.start(this, event.getText(), true);
                break;
            case PUSH:
                if (ApiConfig.get().getSite("push_agent") == null) return;
                DetailActivity.start(this, "push_agent", event.getText(), "", true);
                break;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (Utils.isMenuKey(event)) showDialog();
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Clock.start(mBinding.time, "MM/dd HH:mm:ss");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Clock.get().release();
    }

    @Override
    public void onBackPressed() {
        if (mHistoryPresenter.isDelete()) {
            setHistoryDelete(false);
        } else if (mBinding.recycler.getSelectedPosition() != 0) {
            mBinding.recycler.scrollToPosition(0);
        } else if (!confirm) {
            confirm = true;
            Notify.show(R.string.app_exit);
            App.post(() -> confirm = false, 2000);
        } else {
            super.onBackPressed();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WallConfig.get().clear();
        LiveConfig.get().clear();
        ApiConfig.get().clear();
        Server.get().stop();
    }
}