package com.fongmi.android.tv.ui.fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Button;
import com.fongmi.android.tv.bean.Func;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentHomeBinding;
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
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.collect.Lists;

import java.util.List;


public class HomeFragment extends BaseFragment implements VodPresenter.OnClickListener, FuncPresenter.OnClickListener, HistoryPresenter.OnClickListener {

    public FragmentHomeBinding mBinding;

    private ArrayObjectAdapter mHistoryAdapter;
    public HistoryPresenter mPresenter;
    private ArrayObjectAdapter mAdapter;
    public boolean init;
    private int homeUI;
    private String button;

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
        setRecyclerView();
        setAdapter();
        initEvent();
        init = true;
    }

    @Override
    protected void initData() {
        getHistory();
    }

    protected void initEvent() {
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                if (position < 4) getHomeActicity().showToolBar();
                else getHomeActicity().hideToolBar();
                if (mPresenter != null && mPresenter.isDelete()) setHistoryDelete(false);
            }
        });
    }

    private HomeActivity getHomeActicity() {
        return (HomeActivity) getActivity();
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

    private void setAdapter() {
        ListRow funcRow = getFuncRow();
        if (funcRow != null) mAdapter.add(funcRow);
        if (Setting.isHomeHistory()) mAdapter.add(R.string.home_history);
        mAdapter.add(R.string.home_recommend);
        mHistoryAdapter = new ArrayObjectAdapter(mPresenter = new HistoryPresenter(this));
        homeUI = Setting.getHomeUI();
        button = Setting.getHomeButtons(Button.getDefaultButtons());
    }

    public void addVideo(Result result) {
        int index = getRecommendIndex();
        if (mAdapter.size() > index) mAdapter.removeItems(index, mAdapter.size() - index);
        Style style = result.getStyle(getHome().getStyle());
        for (List<Vod> items : Lists.partition(result.getList(), Product.getColumn(style))) {
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(new VodPresenter(this, style));
            adapter.setItems(items, null);
            mAdapter.add(new ListRow(adapter));
        }
    }

    private ListRow getFuncRow() {
        List<Button> buttonList = Button.getButtons();
        if (buttonList.isEmpty()) return null;
        ArrayObjectAdapter adapter = new ArrayObjectAdapter(new FuncPresenter(this));
        for(int i=0; i<buttonList.size(); i++) {
            adapter.add(Func.create(buttonList.get(i).getResId()));
        }
        if (adapter.size() > 1) {
            ((Func) adapter.get(0)).setNextFocusLeft(((Func) adapter.get(adapter.size() - 1)).getId());
            ((Func) adapter.get(adapter.size() - 1)).setNextFocusRight(((Func) adapter.get(0)).getId());
        }
        return new ListRow(adapter);
    }

    private void refreshFuncRow() {
        if (homeUI == Setting.getHomeUI() && Setting.getHomeButtons(Button.getDefaultButtons()).equals(button)) return;
        if (!TextUtils.isEmpty(button)) mAdapter.removeItems(0, 1);
        homeUI = Setting.getHomeUI();
        button = Setting.getHomeButtons(Button.getDefaultButtons());
        ListRow funcRow = getFuncRow();
        if (funcRow != null) mAdapter.add(0, funcRow);
    }

    public void refreshRecommond() {
        int index = getRecommendIndex();
        mAdapter.notifyArrayItemRangeChanged(index, mAdapter.size() - index);
    }

    public void getHistory() {
        getHistory(false);
    }

    public void getHistory(boolean renew) {
        int historyIndex = getHistoryIndex();
        int recommendIndex = getRecommendIndex();
        if (historyIndex == -1) {
            if (!Setting.isHomeHistory()) return;
            int historyStringIndex = recommendIndex - 1;
            historyStringIndex = historyStringIndex < 0 ? 0 : historyStringIndex;
            mAdapter.add(historyStringIndex, R.string.home_history);
        }
        if (!Setting.isHomeHistory()) {
            mAdapter.removeItems(historyIndex - 1, 2);
            return;
        }
        historyIndex = getHistoryIndex();
        recommendIndex = getRecommendIndex();
        List<History> items = History.get();
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

    @Override
    public void onItemClick(Func item) {
        switch (item.getResId()) {
            case R.string.home_history_short:
                HistoryActivity.start(getActivity());
                break;
            case R.string.home_vod:
                VodActivity.start(getActivity(), getHomeActicity().mResult.clear());
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
        if (mPresenter.isDelete()) {
            new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.dialog_delete_record).setMessage(R.string.dialog_delete_history).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> clearHistory()).show();
        } else {
            setHistoryDelete(true);
        }
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

    @Override
    public void onResume() {
        super.onResume();
        refreshFuncRow();
    }

    public boolean canBack() {
        return mBinding.recycler.getSelectedPosition() != 0;
    }

    public void goBack() {
        mBinding.recycler.scrollToPosition(0);
    }

}
