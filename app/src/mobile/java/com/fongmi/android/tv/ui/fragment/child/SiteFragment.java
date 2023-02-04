package com.fongmi.android.tv.ui.fragment.child;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentSiteBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.activity.BaseFragment;
import com.fongmi.android.tv.ui.adapter.HistoryAdapter;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class SiteFragment extends BaseFragment implements VodAdapter.OnClickListener, HistoryAdapter.OnClickListener {

    private FragmentSiteBinding mBinding;
    private HistoryAdapter mHistoryAdapter;
    private VodAdapter mVodAdapter;

    public static SiteFragment newInstance() {
        return new SiteFragment();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSiteBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setRecyclerView();
        getHistory();
    }

    private void setRecyclerView() {
        mBinding.history.setHasFixedSize(true);
        mBinding.history.addItemDecoration(new SpaceItemDecoration(16));
        mBinding.history.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        mBinding.history.setAdapter(mHistoryAdapter = new HistoryAdapter(this));
        mBinding.recommend.setHasFixedSize(true);
        mBinding.recommend.addItemDecoration(new SpaceItemDecoration(3, 16));
        mBinding.recommend.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mBinding.recommend.setAdapter(mVodAdapter = new VodAdapter(this));
    }

    private void getHistory() {
        mHistoryAdapter.addAll(History.get());
    }

    @Override
    public void onItemClick(Vod item) {
    }

    @Override
    public boolean onLongClick(Vod item) {
        return true;
    }

    @Override
    public void onItemClick(History item) {
    }

    @Override
    public boolean onLongClick(History item) {
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onResult(Result result) {
        mBinding.progressLayout.showContent();
        mVodAdapter.addAll(result.getList());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case VIDEO:
                mVodAdapter.clear();
                mBinding.progressLayout.showProgress();
                break;
            case IMAGE:
                mVodAdapter.notifyDataSetChanged();
                break;
            case HISTORY:
                getHistory();
                break;
        }
    }
}
