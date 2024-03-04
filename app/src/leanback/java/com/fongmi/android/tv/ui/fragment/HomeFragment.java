package com.fongmi.android.tv.ui.fragment;

import android.Manifest;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Func;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.databinding.FragmentHomeBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.activity.CollectActivity;
import com.fongmi.android.tv.ui.activity.HistoryActivity;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.ui.activity.KeepActivity;
import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.ui.activity.PushActivity;
import com.fongmi.android.tv.ui.activity.SearchActivity;
import com.fongmi.android.tv.ui.activity.SettingActivity;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import com.fongmi.android.tv.ui.activity.VodActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.CustomRowPresenter;
import com.fongmi.android.tv.ui.custom.CustomSelector;
import com.fongmi.android.tv.ui.presenter.FuncPresenter;
import com.fongmi.android.tv.ui.presenter.HeaderPresenter;
import com.fongmi.android.tv.ui.presenter.HistoryPresenter;
import com.fongmi.android.tv.ui.presenter.ProgressPresenter;
import com.fongmi.android.tv.ui.presenter.VodPresenter;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.google.common.collect.Lists;
import com.permissionx.guolindev.PermissionX;

import java.util.List;


public class HomeFragment extends BaseFragment implements VodPresenter.OnClickListener, FuncPresenter.OnClickListener, HistoryPresenter.OnClickListener {

    public FragmentHomeBinding mBinding;

    private ArrayObjectAdapter mHistoryAdapter;
    public HistoryPresenter mPresenter;
    private ArrayObjectAdapter mAdapter;
    private SiteViewModel mViewModel;
    private boolean loading;
    private Result mResult;
    private int homeMenuKey;

    private Site getHome() {
        return VodConfig.get().getHome();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentHomeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mBinding.progressLayout.showProgress();
        mResult = Result.empty();
        setRecyclerView();
        setViewModel();
        setAdapter();
        initEvent();
    }

    @Override
    protected void initData() {
        initConfig();
    }

    protected void initEvent() {
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                getActivityHomeBinding().toolbar.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                if (mPresenter != null && mPresenter.isDelete()) setHistoryDelete(false);
            }
        });
    }

    private ActivityHomeBinding getActivityHomeBinding() {
        return getHomeActicity().mBinding;
    }

    private HomeActivity getHomeActicity() {
        return (HomeActivity) getActivity();
    }

    private void checkAction(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            VideoActivity.push(getActivity(), intent.getStringExtra(Intent.EXTRA_TEXT));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            if ("text/plain".equals(intent.getType()) || UrlUtil.path(intent.getData()).endsWith(".m3u")) {
                loadLive("file:/" + FileChooser.getPathFromUri(getActivity(), intent.getData()));
            } else {
                VideoActivity.push(getActivity(), intent.getData().toString());
            }
        }
    }

    private void setRecyclerView() {
        CustomSelector selector = new CustomSelector();
        selector.addPresenter(Integer.class, new HeaderPresenter());
        selector.addPresenter(String.class, new ProgressPresenter());
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), VodPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(22), FuncPresenter.class);
        selector.addPresenter(ListRow.class, new CustomRowPresenter(16), HistoryPresenter.class);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(selector)));
        mBinding.recycler.setVerticalSpacing(ResUtil.dp2px(16));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.result.observe(this, result -> {
            mAdapter.remove("progress");
            addVideo(mResult = result);
            getHomeActicity().setTypes(mResult);
        });
    }

    private void setAdapter() {
        mAdapter.add(getFuncRow());
        mAdapter.add(R.string.home_history);
        mAdapter.add(R.string.home_recommend);
        mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
        homeMenuKey = Setting.getHomeMenuKey();
    }

    private void refreshFuncRow() {
        if (homeMenuKey == Setting.getHomeMenuKey()) return;
        homeMenuKey = Setting.getHomeMenuKey();
        mAdapter.removeItems(0, 1);
        mAdapter.add(0, getFuncRow());
    }

    public void initConfig() {
        if (isLoading()) return;
        WallConfig.get().init();
        LiveConfig.get().init().load();
        VodConfig.get().init().load(getCallback());
        setLoading(true);
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                Notify.dismiss();
                mBinding.progressLayout.showContent();
                checkAction(getActivity().getIntent());
                getHistory();
                getVideo();
                setFocus();
            }

            @Override
            public void error(String msg) {
                Notify.dismiss();
                if (TextUtils.isEmpty(msg) && AppDatabase.getBackup().exists()) onRestore();
                else mBinding.progressLayout.showContent();
                mResult = Result.empty();
                Notify.show(msg);
                setFocus();
            }
        };
    }

    private void onRestore() {
        PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> AppDatabase.restore(new Callback() {
            @Override
            public void success() {
                if (allGranted) initConfig();
                else mBinding.progressLayout.showContent();
            }
        }));
    }

    public void setConfig(Config config) {
        if (config.getUrl().startsWith("file") && !PermissionX.isGranted(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> load(config));
        } else {
            load(config);
        }
    }

    private void load(Config config) {
        switch (config.getType()) {
            case 0:
                Notify.progress(getActivity());
                VodConfig.load(config, getCallback());
                break;
        }
    }

    private void loadLive(String url) {
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                LiveActivity.start(getActivity());
            }
        });
    }

    private void setFocus() {
        setLoading(false);
        App.post(() -> getActivityHomeBinding().title.setFocusable(true), 500);
        if (!getActivityHomeBinding().title.hasFocus()) mBinding.recycler.requestFocus();
    }

    public void getVideo() {
        mResult = Result.empty();
        int index = getRecommendIndex();
        String title = getHome().getName();
        getActivityHomeBinding().title.setText(title.isEmpty() ? ResUtil.getString(R.string.app_name) : title);
        if (mAdapter.size() > index) mAdapter.removeItems(index, mAdapter.size() - index);
        if (getHome().getKey().isEmpty()) return;
        mViewModel.homeContent();
        mAdapter.add("progress");
    }

    private void addVideo(Result result) {
        Style style = result.getStyle(getHome().getStyle());
        for (List<Vod> items : Lists.partition(result.getList(), Product.getColumn(style))) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new VodPresenter(this, style));
            adapter.setItems(items, null);
            mAdapter.add(new ListRow(adapter));
        }
    }

    private ListRow getFuncRow() {
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new FuncPresenter(this));
//        adapter.add(Func.create(R.string.home_vod));
        adapter.add(Func.create(R.string.home_history_short));
        adapter.add(Func.create(R.string.home_live));
        adapter.add(Func.create(R.string.home_search));
        adapter.add(Func.create(R.string.home_keep));
        adapter.add(Func.create(R.string.home_push));
        adapter.add(Func.create(R.string.home_setting));
        ((Func) adapter.get(0)).setNextFocusLeft(((Func) adapter.get(adapter.size() - 1)).getId());
        ((Func) adapter.get(adapter.size() - 1)).setNextFocusRight(((Func) adapter.get(0)).getId());
        return new ListRow(adapter);
    }

    public void refreshRecommond() {
        int index = getRecommendIndex();
        mAdapter.notifyArrayItemRangeChanged(index, mAdapter.size() - index);
    }

    public void getHistory() {
        getHistory(false);
    }

    public void getHistory(boolean renew) {
        List<History> items = History.get();
        int historyIndex = getHistoryIndex();
        int recommendIndex = getRecommendIndex();
        boolean exist = recommendIndex - historyIndex == 2;
        if (renew) mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
        if ((items.isEmpty() && exist) || (renew && exist)) mAdapter.removeItems(historyIndex, 1);
        if ((items.size() > 0 && !exist) || (renew && exist)) mAdapter.add(historyIndex, new ListRow(mHistoryAdapter));
        mHistoryAdapter.setItems(items, null);
    }

    public void setHistoryDelete(boolean delete) {
        mPresenter.setDelete(delete);
        mHistoryAdapter.notifyArrayItemRangeChanged(0, mHistoryAdapter.size());
    }

    private void clearHistory() {
        mAdapter.removeItems(getHistoryIndex(), 1);
        History.delete(VodConfig.getCid());
        mPresenter.setDelete(false);
        mHistoryAdapter.clear();
    }

    private int getHistoryIndex() {
        for (int i = 0; i < mAdapter.size(); i++) if (mAdapter.get(i).equals(R.string.home_history)) return i + 1;
        return -1;
    }

    private int getRecommendIndex() {
        for (int i = 0; i < mAdapter.size(); i++) if (mAdapter.get(i).equals(R.string.home_recommend)) return i + 1;
        return -1;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    @Override
    public void onItemClick(Func item) {
        switch (item.getResId()) {
            case R.string.home_history_short:
                HistoryActivity.start(getActivity());
                break;
            case R.string.home_vod:
                VodActivity.start(getActivity(), mResult.clear());
                break;
            case R.string.home_live:
                LiveActivity.start(getActivity());
                break;
            case R.string.home_search:
                SearchActivity.start(getActivity());
                break;
            case R.string.home_keep:
                KeepActivity.start(getActivity());
                break;
            case R.string.home_push:
                PushActivity.start(getActivity());
                break;
            case R.string.home_setting:
                SettingActivity.start(getActivity());
                break;
        }
    }

    @Override
    public void onItemClick(History item) {
        VideoActivity.start(getActivity(), item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onItemDelete(History item) {
        mHistoryAdapter.remove(item.delete());
        if (mHistoryAdapter.size() > 0) return;
        mAdapter.removeItems(getHistoryIndex(), 1);
        mPresenter.setDelete(false);
    }

    @Override
    public boolean onLongClick() {
        if (mPresenter.isDelete()) clearHistory();
        else setHistoryDelete(true);
        return true;
    }

    @Override
    public void onItemClick(Vod item) {
        if (getHome().isIndexs()) CollectActivity.start(getActivity(), item.getVodName());
        else VideoActivity.start(getActivity(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public boolean onLongClick(Vod item) {
        CollectActivity.start(getActivity(), item.getVodName());
        return true;
    }

}
