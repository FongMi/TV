package com.fongmi.android.tv.ui.fragment.child;

import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.FragmentSiteBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.activity.BaseFragment;
import com.fongmi.android.tv.ui.activity.DetailActivity;
import com.fongmi.android.tv.ui.adapter.HistoryAdapter;
import com.fongmi.android.tv.ui.adapter.VodAdapter;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class SiteFragment extends BaseFragment implements VodAdapter.OnClickListener, HistoryAdapter.OnClickListener {

    private GridLayoutManager mRecommendManager;
    private GridLayoutManager mHistoryManager;
    private HistoryAdapter mHistoryAdapter;
    private FragmentSiteBinding mBinding;
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
        mBinding.progressLayout.showProgress();
        setRecyclerView();
    }

    private void setRecyclerView() {
        mBinding.history.setHasFixedSize(true);
        mBinding.history.getItemAnimator().setChangeDuration(0);
        mBinding.history.setLayoutManager(mHistoryManager = new GridLayoutManager(getContext(), getSpanCount()));
        mBinding.history.setAdapter(mHistoryAdapter = new HistoryAdapter(this));
        mBinding.recommend.setHasFixedSize(true);
        mBinding.recommend.setLayoutManager(mRecommendManager = new GridLayoutManager(getContext(), getSpanCount()));
        mBinding.recommend.setAdapter(mVodAdapter = new VodAdapter(this));
    }

    private void getHistory() {
        mHistoryAdapter.addAll(History.get());
    }

    @Override
    public void onItemClick(Vod item) {
        if (item.shouldSearch()) onLongClick(item);
        else DetailActivity.start(getActivity(), item.getVodId(), item.getVodName());
    }

    @Override
    public boolean onLongClick(Vod item) {
        //CollectActivity.start(this, item.getVodName());
        return true;
    }

    @Override
    public void onItemClick(History item) {
        DetailActivity.start(getActivity(), item.getSiteKey(), item.getVodId(), item.getVodName());
    }

    @Override
    public void onItemDelete(History item) {
        mHistoryAdapter.remove(item.delete());
        mBinding.history.requestLayout();
    }

    @Override
    public boolean onLongClick() {
        setHistoryDelete(true);
        return true;
    }

    private void setHistoryDelete(boolean delete) {
        mHistoryAdapter.setDelete(delete);
        mHistoryAdapter.notifyItemRangeChanged(0, mHistoryAdapter.getItemCount());
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

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mHistoryManager.setSpanCount(getSpanCount());
        mRecommendManager.setSpanCount(getSpanCount());
    }
}
